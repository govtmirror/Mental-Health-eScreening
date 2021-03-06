package gov.va.escreening.assessments.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import gov.va.escreening.constants.RuleConstants;
import gov.va.escreening.context.VeteranAssessmentSmrList;
import gov.va.escreening.delegate.AssessmentDelegate;
import gov.va.escreening.delegate.CreateAssessmentDelegate;
import gov.va.escreening.dto.rule.EventDto;
import gov.va.escreening.entity.Rule;
import gov.va.escreening.entity.Survey;
import gov.va.escreening.entity.SurveyMeasureResponse;
import gov.va.escreening.entity.VeteranAssessment;
import gov.va.escreening.repository.RuleRepository;
import gov.va.escreening.repository.SurveyRepository;
import gov.va.escreening.repository.VeteranAssessmentRepository;
import gov.va.escreening.service.RuleService;
import gov.va.escreening.service.SurveyMeasureResponseService;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;

@Transactional
// this is to ensure all tests do not leave trace, so they are repeatable.
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/spring/root-context.xml" })
public class RuleServiceTest extends AssessmentTestBase {

	static final int OOO_BATTERY_ID = 5;

	Logger logger = LoggerFactory.getLogger(RuleServiceTest.class);

	@Resource
	private AssessmentDelegate assessmentDelegate;

	@Resource
	RuleService ruleService;

	@Resource
	VeteranAssessmentRepository vetAssessmentRepo;

	@Resource
	RuleRepository ruleRepo;

	@Resource
	CreateAssessmentDelegate createAsses;

	@Resource
	SurveyMeasureResponseService surveyMeasureRespSvc;

	@Resource
	SurveyRepository surveyRepo;

	@Resource(name = "veteranAssessmentSmrList")
	VeteranAssessmentSmrList smrLister;

	@Test
	public void testSetup() {
		assertNotNull(assessmentDelegate);
		assertNotNull(vetAssessmentRepo);
	}

	@Test
	public void testRuleRepository() {
		List<Rule> rules = ruleRepo.getRuleForAssessment(1);
		assertNotNull(rules);
	}

	@Test
	@Transactional
	public void testRule103() {
		int ruleId = 103;

		VeteranAssessment assessment = createTestAssessment();
		assertNotNull(assessment);
		vetAssessmentRepo.commit();
		
		Rule r = ruleRepo.findOne(ruleId);

		assertFalse(evaluateRule(assessment.getVeteranAssessmentId(), r));

		SurveyMeasureResponse res = createResponse(240, 2401, true, assessment.getVeteranAssessmentId(), 22);
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(9, 81, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2411, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(10, 93, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2412, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(240, 2402, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(240, 2404, false, assessment.getVeteranAssessmentId(), 22);
		res.setTextValue("test_test");
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2413, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2414, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2415, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		logger.trace("Updating assessment: {}", assessment);
		
		vetAssessmentRepo.update(assessment);
		//this is needed to have the rule service pick up these new responses from the DB 
        //(alternatively we can add this 100% in memory by using the other filterTrue method) 
		vetAssessmentRepo.commit();
		
		// load fresh set of SMR list as they will be read from this cache while evaulating rules
		smrLister.clearSmrFromCache();
		assertTrue(evaluateRule(assessment.getVeteranAssessmentId(), r));
	}

	private boolean evaluateRule(Integer assessmentId, Rule r){
	    return ! ruleService.filterTrue(ImmutableSet.of(r), assessmentId).isEmpty();
	}
	
	@Test
	@Transactional
	public void testRule105() throws InterruptedException {
		final VeteranAssessment assessment = createTestAssessment();
		assertNotNull(assessment);
		
		vetAssessmentRepo.commit();
		
		final Rule r = ruleRepo.findOne(105);

		assertFalse(evaluateRule(assessment.getVeteranAssessmentId(), r));
		
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());
		
		SurveyMeasureResponse res = createResponse(542, 5420, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);
		res = createResponse(542, 5421, true, assessment.getVeteranAssessmentId(), 22);
        assessment.getSurveyMeasureResponseList().add(res);

        res = createResponse(543, 5430, false, assessment.getVeteranAssessmentId(), 22);
        assessment.getSurveyMeasureResponseList().add(res);
        
		res = createResponse(543, 5431, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(544, 5440, false, assessment.getVeteranAssessmentId(), 22);
        assessment.getSurveyMeasureResponseList().add(res);
        
		res = createResponse(544, 5441, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(545, 5450, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);
		
		res = createResponse(545, 5451, false, assessment.getVeteranAssessmentId(), 22);
        assessment.getSurveyMeasureResponseList().add(res);
		
		logger.trace("Updating assessment: {}", assessment);
		
		vetAssessmentRepo.update(assessment);
		//this is needed to have the rule service pick up these new responses from the DB 
		//(alternatively we can add this 100% in memory by using the other filterTrue method) 
		vetAssessmentRepo.commit();

		smrLister.clearSmrFromCache();
		assertTrue(evaluateRule(assessment.getVeteranAssessmentId(), r));

	}

	@Test
	public void testGenerateQuesAndAnswersSingleSelectMatrix() {
		VeteranAssessment assessment = createTestAssessment();

		SurveyMeasureResponse res = createResponse(542, 5421, true, assessment.getVeteranAssessmentId(), 35);
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(543, 5431, true, assessment.getVeteranAssessmentId(), 35);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(544, 5441, true, assessment.getVeteranAssessmentId(), 35);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(545, 5450, true, assessment.getVeteranAssessmentId(), 35);
		assessment.getSurveyMeasureResponseList().add(res);

		vetAssessmentRepo.update(assessment);

		Survey survey = surveyRepo.findOne(35);

		String output = "Test qa output for single select matrix:\n" + surveyMeasureRespSvc.generateQuestionsAndAnswers(survey, assessment.getVeteranAssessmentId());

		logger.info(output);
	}

	// AUDIT-C Screen 1
	@Test
	public void testGenerateQuesAndAnswersSingleSelectMatrixLong() {
		VeteranAssessment assessment = createTestAssessment();
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());

		SurveyMeasureResponse res = createResponse(370, 3700, false, assessment.getVeteranAssessmentId(), 26);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(370, 3701, false, assessment.getVeteranAssessmentId(), 26);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(370, 3702, true, assessment.getVeteranAssessmentId(), 26);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(370, 3703, false, assessment.getVeteranAssessmentId(), 26);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(370, 3704, false, assessment.getVeteranAssessmentId(), 26);
		assessment.getSurveyMeasureResponseList().add(res);

		vetAssessmentRepo.update(assessment);

		Survey survey = surveyRepo.findOne(26);

		String output = "Test qa output for long single select matrix:\n" + surveyMeasureRespSvc.generateQuestionsAndAnswers(survey, assessment.getVeteranAssessmentId());

		logger.info(output);
	}

	/**
	 * Tests qa formatting for 2 multi select questions on the "Prior MH Treatment" page Over the past 18 months has a
	 * Mental Health Provider (i.e. Psychiatrist, Psychologist, Social Worker) diagnosed you with any of the following
	 * In the past 18 months have you had any of the following treatments for your mental health diagnosis?
	 */
	@Test
	public void testGenerateQuesAndAnswersMultiSelect() {
		VeteranAssessment assessment = createTestAssessment();
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());

		// Multi select
		SurveyMeasureResponse res = createResponse(241, 2410, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2411, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2412, false, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2413, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(241, 2414, true, assessment.getVeteranAssessmentId(), 22);
		assessment.getSurveyMeasureResponseList().add(res);

		vetAssessmentRepo.update(assessment);

		Survey survey = surveyRepo.findOne(22);

		String output = "Test qa output for multi select:\n" + surveyMeasureRespSvc.generateQuestionsAndAnswers(survey, assessment.getVeteranAssessmentId());

		logger.info(output);
	}

	/**
	 * Tests qa formatting for multi select matrix question on "Service Injures page 1" Did you have any of the
	 * following injuries at any time during your service?
	 */
	@Test
	public void testGenerateQuesAndAnswersMultiSelectMatrix() {
		VeteranAssessment assessment = createTestAssessment();
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());

		// blast injury
		SurveyMeasureResponse res = createResponse(141, 1411, true, assessment.getVeteranAssessmentId(), 15);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(141, 1412, true, assessment.getVeteranAssessmentId(), 15);
		assessment.getSurveyMeasureResponseList().add(res);

		// Injury to spine or back: none
		res = createResponse(142, 1420, true, assessment.getVeteranAssessmentId(), 15);
		assessment.getSurveyMeasureResponseList().add(res);

		// Nerve damage
		res = createResponse(144, 1441, true, assessment.getVeteranAssessmentId(), 15);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(144, 1442, true, assessment.getVeteranAssessmentId(), 15);
		assessment.getSurveyMeasureResponseList().add(res);

		vetAssessmentRepo.update(assessment);

		Survey survey = surveyRepo.findOne(15);

		String output = "Test qa output for multi select matrix:\n" + surveyMeasureRespSvc.generateQuestionsAndAnswers(survey, assessment.getVeteranAssessmentId());

		logger.info(output);
	}

	/**
	 * Tests qa formatting for freetext question on "Basic demographics page 1" Question: Weight and date
	 */
	@Test
	public void testGenerateQuesAndAnswersFreeTextAndSingleSelect() {
		VeteranAssessment assessment = createTestAssessment();
		assessment.setSurveyMeasureResponseList(new ArrayList<SurveyMeasureResponse>());

		// DOB (in text_value field)
		SurveyMeasureResponse res = createResponse(18, 170, "12/12/2012", assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		// Weight (in number_value field)
		res = createResponse(20, 210, 450, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		// Single select
		res = createResponse(71, 182, false, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(71, 183, false, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(71, 184, false, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(71, 185, true, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		res = createResponse(71, 186, false, assessment.getVeteranAssessmentId(), 3);
		assessment.getSurveyMeasureResponseList().add(res);

		vetAssessmentRepo.update(assessment);

		Survey survey = surveyRepo.findOne(3);

		String output = "Test qa output for freetext and single select page:\n" + surveyMeasureRespSvc.generateQuestionsAndAnswers(survey, assessment.getVeteranAssessmentId());

		logger.info(output);
	}

	@Test
	public void testFullQuesAndAnswers() {
		List<Survey> surveyList = surveyRepo.findForVeteranAssessmentId(56);
		for (Survey s : surveyList) {
			logger.info("==== Survey ID ======" + s.getSurveyId());
			logger.info(surveyMeasureRespSvc.generateQuestionsAndAnswers(s, 56));
		}
	}

	@Test
	public void testGetHealthFactorEvent()
	{
		List<EventDto> eventsByType = ruleService.getEventsByType(RuleConstants.EVENT_TYPE_HEALTH_FACTOR);

		assert(eventsByType.get(0).getName().contains("("));

		eventsByType = ruleService.getEventsByType(RuleConstants.EVENT_TYPE_CONSULT);
		assert(!eventsByType.get(0).getName().contains("("));
	}
}
