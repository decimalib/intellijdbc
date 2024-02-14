# Intellijbc

Provides a set of functions
to simplify the use of [JDBC](https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html) functionality in Kotlin,
enabling more readable and concise code, and proper resource management.

## Usage

For the same query example:

```kotlin
val query = "SELECT * from boards as b where b.ownerId = ? and b.name = ?"
```

<table>
<tr>
<td> Intellijdbc </td> <td> JDBC </td>
</tr>
<tr>
<td>

```kotlin
val stmt = conn.setPreparedStatement(query, ownerId, boardName)
stmt.executeQueryAndClose { rs ->
    require(rs.next()) { "Board not found" }
}
```

</td>
<td>

```kotlin
val stmt = conn.prepareStatement(query)
stmt.setInt(1, ownerId)
stmt.setString(2, boardName)
val rs = stmt.executeQuery()
require(rs.next()) { "Board not found" }
stmt.close()
rs.close()
```

</td>
</tr>
<tr>
<td>

```kotlin
fun doWork() = connection.use { conn ->
    conn.setAtomicTransaction {
        // .. code
    }
}
```

</td>
<td>

```kotlin
fun doWork() = connection.use { conn ->
    try {
        conn.autoCommit = false
        // .. code
        conn.commit()
    } catch (e: Exception) {
        conn.rollback()
        throw e
    } finally {
        conn.autoCommit = true
    }
}
```

</td>
</tr>
</table>

> [!NOTE]
> Reminder:
> [use](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html) function calls the `close` method on the
> receiver object
> when the lambda is finished or an exception is thrown.

## Features

- ✅ **Dynamic creation of statements** - allows the creation of statements with a variable number of parameters;
- ✅ **Type Inferrence** - allows the creation of statements without the need to specify the type of the parameters,
  which is inferred from the parameter types;
- ✅ **Automatic closing of statements and result sets** - automatically closes the statements and result sets after
  usage;
- ✅ **Less verbose** - is less verbose than JDBC, which makes it easier to read and understand;
- ✅ **Support for procedures and functions** - allows the creation of statements for procedures and functions, which are
  used to create and retrieve data from the database;
- ✅ **Inline functions** - some functions are [inline](https://kotlinlang.org/docs/inline-functions.html), which means
  that the code is copied to the call site during compilation, which removes most of the overhead of said function
  calls.

## Motivation

This library was created within a project that strictly prohibited the use of external libraries and dependencies. As
JDBC was the mandated method for database access, these functions were developed to streamline its usage and bring a
more Kotlin-like approach to database interactions.

While this functions offers a convenient and readable way to interact with databases for simple projects, more complex
scenarios might benefit from other libraries like [JDBI](https://jdbi.org/). These libraries provide a wider range of
features and robust functionality for comprehensive database access in both Kotlin and Java.