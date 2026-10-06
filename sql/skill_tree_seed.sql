-- 三级技能树数据（可重复执行）
USE awardsystem;

DELETE FROM skill_tree_node;

-- ============ 方向1：编程与开发 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(1 ,1,NULL,NULL,'编程基础',1,'掌握一种以上编程语言的基本语法与调试能力','Monitor',0,1,'enabled'),
(2 ,1,1   ,1   ,'编程入门',2,'能够独立完成基础语法练习与小程序编写',NULL,0,1,'enabled'),
(3 ,1,1   ,1   ,'面向对象',2,'理解封装、继承、多态，能进行模块化设计',NULL,30,2,'enabled'),
(4 ,1,3   ,1   ,'设计模式',3,'熟练运用常用设计模式重构代码结构',NULL,70,1,'enabled'),
(5 ,1,1   ,3   ,'项目开发',2,'能够参与完整软件项目开发流程',NULL,40,3,'enabled'),
(6 ,1,NULL,NULL,'算法能力',1,'具备算法分析与复杂度评估能力','DataAnalysis',0,2,'enabled'),
(7 ,1,6   ,2   ,'排序与搜索',2,'掌握常见排序、查找算法及其应用场景',NULL,10,1,'enabled'),
(8 ,1,6   ,2   ,'动态规划',2,'能够建立状态转移方程解决最优化问题',NULL,80,2,'enabled'),
(9 ,1,8   ,2   ,'算法竞赛实战',3,'在程序设计类竞赛中独立完成算法题解答',NULL,150,1,'enabled'),
(10,1,NULL,NULL,'项目工程',1,'具备团队协作与工程化交付能力','Briefcase',2,3,'enabled'),
(11,1,10  ,3   ,'项目协作',2,'能在团队中承担明确模块并按时交付',NULL,20,1,'enabled'),
(12,1,10  ,3   ,'独立项目',2,'可独立完成从需求到上线的完整项目',NULL,120,2,'enabled');

-- ============ 方向2：数学分析 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(20,2,NULL,NULL,'数学基础',1,'具备建模所需的数学理论与推导能力','DataAnalysis',0,1,'enabled'),
(21,2,20  ,4   ,'高等数学',2,'掌握微积分、级数与常微分方程',NULL,0,1,'enabled'),
(22,2,20  ,4   ,'线性代数',2,'掌握矩阵运算与线性方程组求解',NULL,30,2,'enabled'),
(23,2,22  ,4   ,'数学建模实战',3,'运用数学工具对实际问题建模与求解',NULL,90,1,'enabled'),
(24,2,NULL,NULL,'数据分析',1,'具备数据清洗、统计分析与结果解读能力','Histogram',0,2,'enabled'),
(25,2,24  ,5   ,'数据清洗',2,'能处理缺失值、异常值并规范数据结构',NULL,20,1,'enabled'),
(26,2,24  ,5   ,'统计推断',2,'掌握假设检验、回归分析与结果解释',NULL,60,2,'enabled'),
(27,2,NULL,NULL,'学术写作',1,'能够规范撰写学术论文与建模报告','Notebook',1,3,'enabled'),
(28,2,27  ,6   ,'论文结构',2,'熟悉摘要、引言、方法、结论的标准结构',NULL,30,1,'enabled'),
(29,2,27  ,6   ,'建模论文撰写',2,'能独立完成完整数学建模论文写作',NULL,80,2,'enabled');

-- ============ 方向3：设计与创意 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(40,3,NULL,NULL,'创意基础',1,'具备创意构思与方案表达能力','MagicStick',0,1,'enabled'),
(41,3,40  ,7   ,'创意策划',2,'能够提出完整创意方案并落地执行',NULL,0,1,'enabled'),
(42,3,41  ,7   ,'品牌策划',3,'能围绕品牌定位输出系统策划方案',NULL,60,1,'enabled'),
(43,3,40  ,10  ,'视觉表达',2,'能运用色彩、版式传递设计意图',NULL,20,2,'enabled'),
(44,3,NULL,NULL,'设计技能',1,'掌握界面与视频等数字化设计工具','Picture',0,2,'enabled'),
(45,3,44  ,11  ,'UI设计',2,'能独立完成界面原型与视觉稿设计',NULL,40,1,'enabled'),
(46,3,44  ,12  ,'视频制作',2,'能完成拍摄、剪辑与成片输出',NULL,30,2,'enabled'),
(47,3,46  ,12  ,'短视频运营',3,'能进行内容策划与传播效果优化',NULL,90,1,'enabled');

-- ============ 方向4：商业与表达 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(60,4,NULL,NULL,'商业思维',1,'具备商业逻辑与市场分析能力','Briefcase',0,1,'enabled'),
(61,4,60  ,8   ,'商业分析',2,'能完成竞品分析与商业可行性论证',NULL,20,1,'enabled'),
(62,4,61  ,8   ,'商业模式设计',3,'能设计并验证可持续的商业模式',NULL,80,1,'enabled'),
(63,4,60  ,8   ,'市场洞察',2,'能基于调研数据提炼市场机会',NULL,50,2,'enabled'),
(64,4,NULL,NULL,'表达呈现',1,'具备公开表达与答辩应对能力','Promotion',0,2,'enabled'),
(65,4,64  ,9   ,'路演表达',2,'能在规定时间内清晰传达项目价值',NULL,20,1,'enabled'),
(66,4,64  ,9   ,'答辩技巧',2,'能从容应对评委提问并有效回应',NULL,60,2,'enabled');

-- ============ 方向5：科研能力 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(80,5,NULL,NULL,'科研素养',1,'具备科研方法与规范意识','Reading',0,1,'enabled'),
(81,5,80  ,5   ,'文献检索',2,'能高效检索、筛选与综述文献',NULL,10,1,'enabled'),
(82,5,80  ,4   ,'实验设计',2,'能设计可重复验证的实验方案',NULL,40,2,'enabled'),
(83,5,NULL,NULL,'成果产出',1,'具备科研成果凝练与发表能力','Trophy',1,2,'enabled'),
(84,5,83  ,6   ,'论文撰写',2,'能独立完成学术论文撰写与投稿',NULL,40,1,'enabled'),
(85,5,84  ,6   ,'高水平论文',3,'产出高水平期刊或会议论文',NULL,120,1,'enabled'),
(86,5,83  ,6   ,'科研项目参与',2,'参与科研课题并承担明确研究任务',NULL,70,2,'enabled');

-- ============ 方向6：电子硬件 ============
INSERT INTO skill_tree_node(node_id,skill_category_id,parent_id,skill_id,name,level,description,icon,unlock_threshold,sort_order,status) VALUES
(100,6,NULL,NULL,'硬件基础',1,'具备电路分析与硬件调试基础能力','Cpu',0,1,'enabled'),
(101,6,100 ,13  ,'电路设计',2,'能完成原理图设计与电路仿真',NULL,0,1,'enabled'),
(102,6,100 ,13  ,'元器件选型',2,'能根据需求合理选型与验证器件',NULL,30,2,'enabled'),
(103,6,NULL,NULL,'嵌入式开发',1,'具备嵌入式系统开发与集成能力','Cpu',0,2,'enabled'),
(104,6,103 ,15  ,'单片机应用',2,'能完成单片机程序编写与外设驱动',NULL,20,1,'enabled'),
(105,6,103 ,14  ,'嵌入式开发',2,'能进行嵌入式系统开发与移植',NULL,50,2,'enabled'),
(106,6,105 ,14  ,'系统集成',3,'能完成软硬件联调与系统级集成',NULL,110,1,'enabled');

SELECT skill_category_id, COUNT(*) nodes, SUM(level=1) l1, SUM(level=2) l2, SUM(level=3) l3 FROM skill_tree_node GROUP BY skill_category_id;