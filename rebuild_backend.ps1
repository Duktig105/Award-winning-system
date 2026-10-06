# 1. 重新解压jar依赖到临时目录
$libDir = 'D:\system\rebuild_lib'
if (Test-Path $libDir) { Remove-Item -Recurse -Force $libDir }
New-Item -ItemType Directory -Path $libDir | Out-Null
Set-Location $libDir
& 'D:\Java\jdk-21\bin\jar.exe' -xf 'D:\system\springboot-backend\target\springboot-backend-0.0.1-SNAPSHOT.jar' 'BOOT-INF/lib'

# 2. 组装classpath
$libJars = Get-ChildItem -Path "$libDir\BOOT-INF\lib" -Filter *.jar | ForEach-Object { $_.FullName }
$cp = ($libJars -join ';') + ';D:\system\springboot-backend\target\classes'

# 3. 编译所有源文件（-parameters 保留参数名）
$sources = Get-ChildItem -Path 'D:\system\springboot-backend\src\main\java' -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$proc = Start-Process -FilePath 'D:\Java\jdk-21\bin\javac.exe' -ArgumentList (@('-parameters','-d','D:\system\springboot-backend\target\classes','-cp',$cp) + $sources) -NoNewWindow -PassThru -Wait -RedirectStandardOutput "$libDir\out.txt" -RedirectStandardError "$libDir\err.txt"
$exit = $proc.ExitCode
Write-Host "javac exit: $exit"
if ($exit -ne 0) {
    Get-Content "$libDir\err.txt" -TotalCount 40 -Encoding UTF8
    exit 1
}

# 4. 更新jar内 BOOT-INF/classes
$pkgDir = 'D:\system\rebuild_pkg'
if (Test-Path $pkgDir) { Remove-Item -Recurse -Force $pkgDir }
New-Item -ItemType Directory -Path "$pkgDir\BOOT-INF\classes" | Out-Null
Copy-Item -Path 'D:\system\springboot-backend\target\classes\*' -Destination "$pkgDir\BOOT-INF\classes" -Recurse -Force
Set-Location $pkgDir
& 'D:\Java\jdk-21\bin\jar.exe' uf 'D:\system\springboot-backend\target\springboot-backend-0.0.1-SNAPSHOT.jar' 'BOOT-INF'
Write-Host 'jar updated'
