1、前后端分离模式，服务端Java Spring Boot框架，MVC分层架构，controller调用service，servic调用dao, 不要跨越层调用，使用导入类名。
2、服务端Controller负责接口请求，restful风格，调用Service层处理业务，不要出现业务代码。返回响应为json格式，包含code、msg、data三部分，code为状态码，msg为状态描述，data为返回数据，返回数据字段超过2个就抽象为对象，以Dto结尾，查询请求参数超过3个字段就抽象为对象，以Query结尾、更新，编辑请求参数超过3个字段就抽象为对象，以Param结尾。
3、Service层负责处理业务逻辑，调用Dao层操作数据库，不要出现数据库操作语句，返回业务数据。
4、Dao层负责数据库操作，封装数据库操作语句，使用MyBatis-Plus框架，数据库为PostgreSQL，表结构设计符合数据库 normalization 范式。数据库表字段命名采用下划线命名法，主键字段命名为id，外键字段命名为${表名}_id，索引字段命名为${表名}_${字段名}_idx。
5、异常抛出使用Warning.java,在ErrorCode.java中定义异常枚举，有ExceptionAdvice处理异常，返回给前端。
6、前端页面使用Vue 3 + Vite框架，负责展示数据和与用户交互，使用Element Plus组件库，页面布局采用Flex布局，响应式设计，适配PC端和移动端。
7、代码要尽量复用，避免重复编写相同的逻辑，如使用通用的组件、工具类、方法等。