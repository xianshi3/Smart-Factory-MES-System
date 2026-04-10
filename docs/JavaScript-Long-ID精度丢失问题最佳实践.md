# 解决 JavaScript 处理 Long 类型 ID 精度丢失问题全攻略

## 前言

在前后端分离的项目中，我们经常会遇到一个棘手的问题：后端使用 Long 类型的主键（如雪花算法生成的 ID）在传递到前端时会发生精度丢失，导致查询、删除等操作失败。本文将从问题原理到解决方案，全面讲解这个问题的最佳实践。

## 问题现象

### 数据库中的 ID

```
id: 2042490503953403905
```

### 前端接收到的 ID

```
2042490503953404000  // 最后几位变了！
```

### 错误表现

- 删除操作返回 "记录不存在"
- 编辑时查询不到数据
- 列表显示正常，但点击详情失败

## 问题根源

### JavaScript Number 的安全整数范围

JavaScript 使用 IEEE 754 双精度浮点数表示数字，其安全整数范围为：

```
-2^53 + 1 到 2^53 - 1
即：-9007199254740991 到 9007199254740991
```

而雪花算法生成的 ID 通常是 64 位整数（最大约 9.2 × 10^18），远超过这个范围。

### 精度丢失原理

```
2042490503953403905
转换为二进制：
11101011100010101100101011001110001001000000000000001

JavaScript 只能精确表示 53 位整数，
超出的部分会被四舍五入，导致精度丢失。
```

## 解决方案

### 方案一：后端序列化时转为 String（推荐）

在实体类的 ID 字段上添加 `@JsonSerialize` 注解：

```java
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonSerialize(using = ToStringSerializer.class)
private Long id;
```

**优点**：
- 后端改动最小
- JSON 输出变为字符串 `"2042490503953403905"`
- 前端无需改造

**缺点**：
- 需要确保所有接收 ID 的地方都能处理字符串

### 方案二：前端接收后转为 String

不推荐，因为问题在前端接收到数据时就已经发生了。

### 方案三：后端使用 String 类型主键

将数据库主键改为 VARCHAR 类型，但会失去自增特性，不推荐。

## 完整改造示例（Spring Boot + Vue）

### 1. 后端改造

**实体类 (Java)**

```java
@Data
public class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
}
```

**DTO 也需要相同处理**

```java
public class WorkOrderDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
}
```

### 2. 前端改造

**TypeScript 类型定义**

```typescript
// 之前
export function deleteWorkOrder(id: number)

// 之后 - 兼容 string 和 number
export function deleteWorkOrder(id: string | number) {
  return request({ url: `/workorder/${id}`, method: 'delete' })
}
```

**注意**：URL 路径中的 `id` 会被自动转为字符串，无需手动转换。

### 3. 后端接收 String 类型 ID（可选）

如果前端传 String ID，后端可以用 String 接收后在服务层转换：

```java
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable String idStr) {
    Long id = Long.parseLong(idStr);
    // 或者使用 Spring 的 Converter
}
```

## 相关问题扩展

### 1. 乐观锁 Version 冲突

使用 MyBatis-Plus 的 `@Version` 注解时，手动 `updateById()` 会触发乐观锁检查，可能导致异常。

**解决**：使用 `UpdateWrapper`

```java
var updateWrapper = new UpdateWrapper<WorkOrder>()
        .set("deleted", 1)
        .eq("id", id);
mapper.update(null, updateWrapper);
```

### 2. 大数字在数据库查询

直接用 JavaScript 发送的 ID 查询数据库会失败，因为 ID 已经被截断。

**解决**：确保使用修复后的 String ID 或 Long ID。

## 参考资料

- [JavaScript Number 精度问题](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER)
- [Jackson ToStringSerializer](https://github.com/FasterXML/jackson-databind/wiki/JacksonSerizers)
- [MyBatis-Plus 雪花算法](https://baomidou.com/pages/994cce/)