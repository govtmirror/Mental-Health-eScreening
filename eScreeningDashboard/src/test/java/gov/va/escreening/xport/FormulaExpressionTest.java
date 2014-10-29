package gov.va.escreening.xport;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class FormulaExpressionTest {
	Logger logger = LoggerFactory.getLogger(FormulaExpressionTest.class);

	@Test
	public void formula1() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "( ( (true?1:0 + false?1:0 + false?1:0) )  +  ( (true?1:0 + false?1:0 + false?1:0 + false?1:0 + false?1:0) )  + (false?1:0) + (false?1:0))";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("2"));
	}

	@Test
	public void formula0() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "((false?1:0) + (false?1:0))";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("0"));
	}

	@Test
	public void formula2() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "( ( (true?1:0 + false?1:0 + false?1:0) )  +  ( (true?1:0 + false?1:0 + false?1:0 + false?1:0 + false?1:0) ) )";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("2"));
	}

	@Test
	public void formula3() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "( ( (true?1:0 + false?1:0 + false?1:0 + false?1:0 + false?1:0) )  )";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("1"));
	}

	@Test
	public void formula4() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "( ( (true?1:0 + false?1:0 + false?1:0) ) )";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("1"));
	}
	@Test
	public void formula5() {
		ExpressionParser parser = new SpelExpressionParser();
		String mathFormula = "((true?1:0) + (true?1:0) + (true?1:0) + (true?1:0) + (true?1:0) + (true?1:0))";
		Assert.assertTrue(parser.parseExpression(mathFormula).getValue(String.class).equals("6"));
	}

	@Test
	public void simple() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("'Spring Expression Language Tutorial'");
		Assert.assertTrue(exp.getValue(String.class).equals("Spring Expression Language Tutorial"));
	}
	@Test
	public void simple1() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("\"Spring Expression Language Tutorial\"");
		Assert.assertTrue(exp.getValue(String.class).equals("Spring Expression Language Tutorial"));
	}
}