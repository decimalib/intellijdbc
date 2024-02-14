package decima

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * Ensures the [transaction] block received is executed as a single unit of work. Atomic transactions
 * ensure that either all the SQL statements within the transaction are executed successfully and
 * the changes are committed to the database, or none of them are executed and the changes are rolled back.
 * This guarantees that the data is left in a consistent state, even if an error or failure occurs
 * during the transaction.
 * @param transaction a set of SQL statements.
 * @return the result of the [transaction] block.
 */
inline fun <T> Connection.setAtomicTransaction(crossinline transaction: () -> T): T =
    runCatching {
        autoCommit = false
        val result = transaction()
        commit()
        result
    }.onFailure {
        rollback()
    }.also {
        autoCommit = true
        close()
    }.getOrThrow()

/**
 * Sets all parameters of an **SQL statement** by the received declaration order
 * without the need to indicate the parameter type.
 *
 * Example of usage:
 * ```
 * val query = "SELECT * from student as s where s.name = ? and s.age = ?"
 * val statement = connection.setPreparedStatement(query, "John", 20)
 * // which equals to this JDBC code:
 * val statement = connection.prepareStatement(query)
 * statement.setString(1, "John")
 * statement.setInt(2, 20)
 * ```
 * @param query SQL query as a string.
 * @param parameters parameters to set.
 * @return a complete [PreparedStatement].
 *
 */
fun <T : Any?> Connection.setPreparedStatement(
    query: String,
    vararg parameters: T
): PreparedStatement {
    val statement = prepareStatement(query)
    setStatementParameters(parameters, statement)
    return statement
}

/**
 * Prepares a **stored procedure** or **function** with all parameters set by the received declaration order,
 * without the need to indicate the parameter type.
 *
 * Example of usage:
 * ```
 * val statement = connection.setCallableStatement(
 *      "getStudent", studentId, classId
 * )
 * // which equals to this JDBC code:
 * val statement = connection.prepareCall("{ call getStudent(?, ?) }")
 * statement.setObject(1, studentId)
 * statement.setObject(2, classId)
 * ```
 * @param routineName name of the stored procedure or function.
 * @param parameters parameters to set.
 * @return a complete [CallableStatement].
 * @see [setCallableStatement], [executeAndClose]
 */
fun <T : Any?> Connection.setCallableStatement(
    routineName: String,
    vararg parameters: T
): CallableStatement {
    // (?, ..., ?)
    val questionMarks = parameters.joinToString(", ", "(", ")") { "?" }
    // Example: { call getStudent(?, ?) }
    val sqlString = "{ call $routineName $questionMarks }"
    val statement = prepareCall(sqlString)
    setStatementParameters(parameters, statement)
    return statement
}

/**
 * Sets all parameters of an SQL statement by the received declaration order without the need to indicate the parameter type.
 * @param parameters parameters to set.
 * @param statement a complete [PreparedStatement].
 */
private fun <T : Any?> setStatementParameters(parameters: Array<out T>, statement: PreparedStatement) {
    parameters.forEachIndexed { index, param ->
        statement.setObject(index + 1, param)
    }
}

/**
 * Closes a [PreparedStatement] immediately after executing an update statement.
 * @see , [executeQueryAndClose].
 */
fun PreparedStatement.executeUpdateAndClose(): Int =
    executeUpdate().also { use {} }

/**
 * Closes a [PreparedStatement] and the created [ResultSet] immediately after executing the [block] function.
 * This method is not recommended if the [ResultSet] is to be kept after,
 * for that use the regular [PreparedStatement.executeQuery] and close the [PreparedStatement] manually after.
 *
 * Example of usage:
 * ```
 * preparedstatement.executeQueryAndClose { rs ->
 *      while(rs.next()) {
 *          println(rs.getString("name"))
 *      }
 * }
 * ```
 * @param block function to handle the [ResultSet].
 * @return the result of the [block] function.
 * @see , [executeUpdateAndClose]
 */
inline fun <reified T> PreparedStatement.executeQueryAndClose(crossinline block: (ResultSet) -> T): T =
    use { stmt ->
        stmt.executeQuery().use { rs ->
            block(rs)
        }
    }

/**
 * Closes a [CallableStatement] immediately after executing a procedure or function that is
 * defined in the database.
 *
 * Example of usage:
 * ```
 * val statement = connection.setCallableStatement(
 *     "getStudent", studentId, classId
 * )
 * val result = statement.executeAndClose()
 * ```
 * @return true if the first result is a [ResultSet] object; false if it is an update count or there are no results.
 * @see [setCallableStatement]
 */
fun CallableStatement.executeAndClose(): Boolean =
    execute().also { use {} }

/**
 * Closes a [Statement] immediately after executing the received script that may contain one or more SQL-completed statements.
 *
 * Example of usage:
 * ```
 * val script = """
 *     CREATE TABLE student (
 *     id INT PRIMARY KEY,
 *     name VARCHAR(255) NOT NULL
 *     );
 *     INSERT INTO student (id, name) VALUES (1, 'John');
 *     INSERT INTO student (id, name) VALUES (2, 'Mary');
 *     INSERT INTO student (id, name) VALUES (3, 'Paul');
 * """.trimIndent()
 * val result = connection.createStatement().executeScriptAndClose(script)
 * ```
 * @param script SQL script as a string.
 * @return true if the first result is a [ResultSet] object; false if it is an update count or there are no results.
 * @see [executeAndClose], [executeQueryAndClose], [executeUpdateAndClose]
 */
fun Statement.executeScriptAndClose(script: String): Boolean =
    execute(script).also { use {} }
