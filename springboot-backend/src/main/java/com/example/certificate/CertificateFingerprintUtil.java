package com.example.certificate;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 证书图片指纹工具类
 * 1. SHA-256 文件哈希（判断完全重复）
 * 2. 图片格式转换与标准化（统一转 JPEG、限制最长边）
 * 3. pHash 感知哈希（DCT 8x8，判断内容相似）
 */
public final class CertificateFingerprintUtil {

    private CertificateFingerprintUtil() {
    }

    /** 允许上传的证书文件类型 */
    public static final java.util.Set<String> ALLOWED_EXTS =
            java.util.Set.of("jpg", "jpeg", "png", "bmp", "webp", "pdf");

    /** 单文件大小上限：10MB */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** 标准化图片最长边 */
    private static final int STD_MAX_SIDE = 512;

    /** pHash 采样尺寸 */
    private static final int HASH_SIZE = 32;
    private static final int HASH_BLOCK = 8;

    /** 计算文件 SHA-256（十六进制小写） */
    public static String sha256Hex(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 是否为支持的图片类型（可做pHash） */
    public static boolean isImage(String ext) {
        if (ext == null) return false;
        String e = ext.toLowerCase().replace(".", "");
        return e.equals("jpg") || e.equals("jpeg") || e.equals("png") || e.equals("bmp") || e.equals("webp");
    }

    public static boolean isAllowed(String ext) {
        if (ext == null) return false;
        return ALLOWED_EXTS.contains(ext.toLowerCase().replace(".", ""));
    }

    /**
     * 图片标准化：读取任意格式图片，等比缩放到最长边 512px，统一输出为 JPEG
     * 返回 null 表示图片无法解码（损坏或不支持的格式）
     */
    public static BufferedImage standardize(File src) throws IOException {
        BufferedImage origin = ImageIO.read(src);
        if (origin == null) return null;
        int w = origin.getWidth();
        int h = origin.getHeight();
        double scale = Math.min(1.0, (double) STD_MAX_SIDE / Math.max(w, h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));
        BufferedImage target = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origin, 0, 0, tw, th, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /**
     * 计算 pHash（感知哈希）：32x32 灰度 -> DCT -> 左上 8x8（去掉DC） -> 中位数比较 -> 64bit
     * 返回 16 位 hex 字符串
     */
    public static String phash(BufferedImage stdImage) {
        if (stdImage == null) return null;
        // 1. 缩放到 32x32 灰度矩阵
        double[][] gray = new double[HASH_SIZE][HASH_SIZE];
        BufferedImage small = new BufferedImage(HASH_SIZE, HASH_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(stdImage, 0, 0, HASH_SIZE, HASH_SIZE, null);
        } finally {
            g.dispose();
        }
        for (int y = 0; y < HASH_SIZE; y++) {
            for (int x = 0; x < HASH_SIZE; x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, gr = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                gray[y][x] = 0.299 * r + 0.587 * gr + 0.114 * b;
            }
        }
        // 2. 二维 DCT
        double[][] dct = dct2d(gray);
        // 3. 取左上 8x8，排除 [0][0]（直流分量）
        double[] block = new double[HASH_BLOCK * HASH_BLOCK - 1];
        int idx = 0;
        for (int y = 0; y < HASH_BLOCK; y++) {
            for (int x = 0; x < HASH_BLOCK; x++) {
                if (x == 0 && y == 0) continue;
                block[idx++] = dct[y][x];
            }
        }
        // 4. 中位数阈值生成 64bit
        double[] sorted = block.clone();
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        long hash = 0;
        for (double v : block) {
            hash = (hash << 1) | (v > median ? 1L : 0L);
        }
        return String.format("%016x", hash);
    }

    /** 计算两个 pHash 的汉明距离 */
    public static int hammingDistance(String phash1, String phash2) {
        if (phash1 == null || phash2 == null || phash1.length() != phash2.length()) {
            return Integer.MAX_VALUE;
        }
        long a = Long.parseUnsignedLong(phash1, 16);
        long b = Long.parseUnsignedLong(phash2, 16);
        return Long.bitCount(a ^ b);
    }

    /** 由汉明距离计算相似度（0~1） */
    public static double similarity(String phash1, String phash2) {
        int dist = hammingDistance(phash1, phash2);
        if (dist == Integer.MAX_VALUE) return 0;
        return (64.0 - dist) / 64.0;
    }

    /** 简单二维 DCT-II 变换（N=32，规模小，直接计算） */
    private static double[][] dct2d(double[][] matrix) {
        int n = matrix.length;
        double[][] cosTable = new double[n][n];
        for (int k = 0; k < n; k++) {
            for (int x = 0; x < n; x++) {
                cosTable[k][x] = Math.cos((2 * x + 1) * k * Math.PI / (2.0 * n));
            }
        }
        double[][] tmp = new double[n][n];
        // 行变换
        for (int y = 0; y < n; y++) {
            for (int k = 0; k < n; k++) {
                double sum = 0;
                for (int x = 0; x < n; x++) {
                    sum += matrix[y][x] * cosTable[k][x];
                }
                tmp[y][k] = sum * (k == 0 ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n));
            }
        }
        // 列变换
        double[][] result = new double[n][n];
        for (int x = 0; x < n; x++) {
            for (int k = 0; k < n; k++) {
                double sum = 0;
                for (int y = 0; y < n; y++) {
                    sum += tmp[y][x] * cosTable[k][y];
                }
                result[k][x] = sum * (k == 0 ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n));
            }
        }
        return result;
    }
}
