# 中文编码问题分析

## 问题描述

前端通过API获取菜单数据时，中文字符显示为乱码（如 `æŠ¥è­¦ç®¡ç†`），但数据库中存储的中文是正确的。

## 问题根因分析

### 根本原因

Java后端与MySQL数据库的字符编码配置不一致。

JDBC连接参数使用了 `utf8mb4`，这是无效的Java字符集名称：

```
java.sql.SQLException: Unsupported character encoding 'utf8mb4'
```

MySQL Connector/J 8.0需要使用标准的字符集名称如 `utf8`。

### 技术细节

MySQL字符集：
- `utf8mb4` - MySQL的UTF-8编码(完整4字节)
- `utf8` - MySQL的UTF-8编码(最多3字节)

问题链条：
1. 数据库表使用 `utf8mb4` 字符集存储中文
2. JDBC连接使用错误的characterEncoding参数
3. Java读取数据时编码不匹配
4. JSON序列化时继续使用错误的编码
5. 前端接收到乱码

## 解决方案

### 方案一：修改JDBC连接参数

```yaml
url: jdbc:mysql://localhost:3306/mes_db?useUnicode=true&characterEncoding=utf8&...
```

使用 `utf8` 而不是 `utf8mb4`。

### 方案二：前端绕过

前端直接使用本地菜单配置：

```typescript
const loadMenus = async () => {
  menus.value = defaultMenus
  loading.value = false
}
```

## 经验总结

1. 字符编码一致性：整个技术栈（数据库、JDBC、Java、JSON）必须使用一致的编码
2. 标准名称：使用标准的字符集名称（如 UTF-8、utf8）而不是数据库特定的名称
3. 调试方法：检查数据库直接存储的数据、检查API返回的原始JSON、使用HEX编码查看实际字节

## 参考资料

- [MySQL字符集文档](https://dev.mysql.com/doc/refman/8.0/en/charset.html)
- [JDBC字符编码配置](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-charsets.html)