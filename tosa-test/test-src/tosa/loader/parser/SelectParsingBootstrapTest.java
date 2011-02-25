package tosa.loader.parser;

import org.junit.Test;
import tosa.loader.parser.mysql.MySQL51SQLParser;
import tosa.loader.parser.tree.SelectStatement;
import tosa.loader.parser.tree.VariableExpression;

import java.util.List;

import static org.junit.Assert.*;

public class SelectParsingBootstrapTest {
  @Test public void bootstrapSelectTest() {
    MySQL51SQLParser parser = new MySQL51SQLParser();
    SelectStatement select = parser.parseSQLFile(null, "SELECT * FROM foo WHERE foo.bar = 10");
    assertNotNull(select);
    assertNotNull(select.firstToken().toString());
    String sql = select.toSQL();
    System.out.println(sql);
    assertNotNull(sql);
  }

  @Test public void bootstrapVariableTest() {
    MySQL51SQLParser parser = new MySQL51SQLParser();
    SelectStatement select = parser.parseSQLFile(null, "SELECT * \n" +
                                                 "FROM foo \n" +
                                                 "WHERE foo.bar = :val");
    assertNotNull(select);
    assertNotNull(select.firstToken().toString());
    String sql = select.toSQL();
    System.out.println(sql);
    assertNotNull(sql);
    List<VariableExpression> vars = select.findDescendents(VariableExpression.class);
    VariableExpression var = vars.get(0);
    assertNotNull(var);
    assertEquals(":val", var.getName());
    assertEquals("?", var.toString());
  }
}