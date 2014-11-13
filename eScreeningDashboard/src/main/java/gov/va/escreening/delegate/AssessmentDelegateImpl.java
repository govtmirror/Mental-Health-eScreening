package gov.va.escreening.delegate;

import static gov.va.escreening.constants.AssessmentConstants.ASSESSMENT_EVENT_MARKED_COMPLETED;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_CALL_TIME;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_EMAIL;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_FIRST_NAME_ID;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_LAST_NAME_ID;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_MIDDLE_NAME_ID;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_PHONE_;
import static gov.va.escreening.constants.AssessmentConstants.MEASURE_IDENTIFICATION_SSN_LAST_FOUR;
import static gov.va.escreening.constants.AssessmentConstants.PERSON_TYPE_VETERAN;
import static gov.va.escreening.constants.AssessmentConstants.SURVEY_IDENTIFICATION_ID;
import gov.va.escreening.context.AssessmentContext;
import gov.va.escreening.context.VeteranAssessmentSmrList;
import gov.va.escreening.domain.AssessmentStatusEnum;
import gov.va.escreening.domain.VeteranDto;
import gov.va.escreening.domain.VeteranDtoHelper;
import gov.va.escreening.dto.ae.Answer;
import gov.va.escreening.dto.ae.AssessmentRequest;
import gov.va.escreening.dto.ae.AssessmentResponse;
import gov.va.escreening.dto.ae.CompletionResponse;
import gov.va.escreening.dto.ae.Measure;
import gov.va.escreening.entity.AssessmentStatus;
import gov.va.escreening.entity.SurveySection;
import gov.va.escreening.entity.VeteranAssessment;
import gov.va.escreening.entity.VeteranAssessmentAuditLog;
import gov.va.escreening.entity.VeteranAssessmentAuditLogHelper;
import gov.va.escreening.exception.InvalidAssessmentContextException;
import gov.va.escreening.repository.AssessmentStatusRepository;
import gov.va.escreening.repository.SurveyRepository;
import gov.va.escreening.repository.SurveySectionRepository;
import gov.va.escreening.repository.VeteranAssessmentAuditLogRepository;
import gov.va.escreening.repository.VeteranAssessmentRepository;
import gov.va.escreening.service.AssessmentEngineService;
import gov.va.escreening.service.VeteranAssessmentService;
import gov.va.escreening.service.VeteranAssessmentSurveyService;
import gov.va.escreening.service.VeteranService;
import gov.va.escreening.templateprocessor.TemplateProcessorService;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

@Transactional
public class AssessmentDelegateImpl implements AssessmentDelegate {

	private static final Logger logger = LoggerFactory.getLogger(AssessmentDelegate.class);

	@Autowired
	private AssessmentContext assessmentContext;
	@Autowired
	private VeteranAssessmentService veteranAssessmentService;
	@Autowired
	private VeteranAssessmentSurveyService veteranAssessmentSurveyService;
	@Autowired
	private VeteranService veteranService;
	@Autowired
	private AssessmentEngineService assessmentEngineService;
	@Autowired
	private VeteranAssessmentAuditLogRepository veteranAssessmentAuditLogRepository;
	@Autowired
	private VeteranAssessmentRepository veteranAssessmentRepository;
	@Autowired
	private AssessmentStatusRepository assessmentStatusRepository;
	@Autowired
	private SurveyRepository surveyRepository;
	@Autowired
	private SurveySectionRepository surveySectionRepository;

	@Resource(type = TemplateProcessorService.class)
	private TemplateProcessorService templateProcessorService;

	@Override
	public List<VeteranDto> findVeterans(VeteranDto veteran) {
		logger.debug("findVeterans");
		return veteranService.findVeterans(veteran);
	}

	@Override
	public VeteranAssessment getAvailableVeteranAssessment(Integer veteranId) {

		List<VeteranAssessment> veteranAssessments = veteranAssessmentService.getAvailableAssessmentsForVeteran(veteranId);

		if (veteranAssessments == null || veteranAssessments.size() < 1) {
			return null;
		} else {
			for (VeteranAssessment assessment : veteranAssessments) {
				if (surveyRepository.findForVeteranAssessmentId(assessment.getVeteranAssessmentId()).size() > 0)
					return assessment;
			}
			return null;
		}
	}

	@Override
	public void setUpAssessmentContext(VeteranDto veteran,
			VeteranAssessment veteranAssessment) {
		logger.debug("Set up assessment context");

		assessmentContext.setVeteran(veteran);
		assessmentContext.setVeteranAssessmentId(veteranAssessment.getVeteranAssessmentId());
		assessmentContext.setIsInitialized(true);
	}

	@Override
	public void ensureValidAssessmentContext() throws InvalidAssessmentContextException {
		logger.debug("Ensuring valid assessment context");
		if (!assessmentContext.getIsInitialized() || assessmentContext.getVeteran() == null)
			throw new InvalidAssessmentContextException("Veteran not logged in");
	}

	@Override
	public List<SurveySection> getAssessmentSections() {
		logger.debug("Getting surveys for current assessment");
		ensureValidAssessmentContext();

		return surveySectionRepository.findForVeteranAssessmentId(assessmentContext.getVeteranAssessmentId());
	}

	@Override
	public AssessmentResponse processPage(AssessmentRequest assessmentRequest) {

		if (assessmentRequest != null) {
			logger.debug("Assessment request: {}", assessmentRequest);

			// we set the assessment ID from the context (not from the request)
			assessmentRequest.setAssessmentId(assessmentContext.getVeteranAssessmentId());
		}

		AssessmentResponse response = assessmentEngineService.processPage(assessmentRequest);

		prepopulateResponseAnswers(response);

		return response;
	}

	@Override
	public Integer getVeteranAssessmentId() {
		return assessmentContext.getVeteranAssessmentId();
	}

	private void prepopulateResponseAnswers(AssessmentResponse response) {
		// Check to see if this is the identification survey, if it is then pre-populate the response answers
		VeteranDto veteran = assessmentContext.getVeteran();
		if (veteran != null && response.getAssessment() != null && response.getAssessment().getCurrentSurveyId() == SURVEY_IDENTIFICATION_ID) {
			for (Measure measure : response.getPage().getMeasures()) {

				String responseText = null;
				if (!measure.getAnswers().isEmpty() && measure.getAnswers().get(0) != null) {
					switch (measure.getMeasureId()) {

					case MEASURE_IDENTIFICATION_FIRST_NAME_ID:
						responseText = veteran.getFirstName();
						break;
					case MEASURE_IDENTIFICATION_MIDDLE_NAME_ID:
						responseText = veteran.getMiddleName();
						break;
					// case MEASURE_IDENTIFICATION_SUFFIX_ID:
					// responseText = veteran.getSuffix();
					// break;
					case MEASURE_IDENTIFICATION_LAST_NAME_ID:
						responseText = veteran.getLastName();
						break;
					case MEASURE_IDENTIFICATION_SSN_LAST_FOUR:
						responseText = veteran.getSsnLastFour();
						break;
					case MEASURE_IDENTIFICATION_EMAIL:
						responseText = veteran.getEmail();
						break;
					case MEASURE_IDENTIFICATION_PHONE_:
						responseText = VeteranDtoHelper.getPhoneByPriority(veteran);
						break;
					case MEASURE_IDENTIFICATION_CALL_TIME:
						if (!Strings.isNullOrEmpty(veteran.getBestTimeToCall())) {
							// pre populate best time to call
							try {
								String prevResponse = veteran.getBestTimeToCall();
								for (Answer answer : measure.getAnswers()) {
									if (prevResponse.equalsIgnoreCase(answer.getAnswerText())) {
										answer.setAnswerResponse("true");
										answer.setOtherAnswerResponse(veteran.getBestTimeToCallOther());
									}
								}
							} catch (NumberFormatException e) {
								logger.error("Error parsing previously saved call time measure answer", e);
							}
						}
						return;
					}

					if (responseText != null) {
						measure.getAnswers().get(0).setAnswerResponse(responseText);
					}
				}

			}
		}
	}

	@Override
	public String getVeteranFullName() {
		if (assessmentContext.getVeteran() == null) {
			return null;
		}

		if (StringUtils.isNotBlank(assessmentContext.getVeteran().getFirstName())) {
			return assessmentContext.getVeteran().getLastName() + ", " + assessmentContext.getVeteran().getFirstName();
		} else {
			return assessmentContext.getVeteran().getLastName();
		}
	}

	@Override
	public CompletionResponse getCompletionResponse(int batteryId) {
		CompletionResponse response = new CompletionResponse();
		response.setCompletionText(templateProcessorService.generateCompletionMsgFor(batteryId));
		return response;
	}

	@Override
	public String getWelcomeMessage()
	{
		VeteranAssessment veteranAssessment = veteranAssessmentRepository.findOne(assessmentContext.getVeteranAssessmentId());
		return veteranAssessment.getBattery().getWelcomeMessage();
		
	}
	
	@Override
	public void markAssessmentAsComplete() {
		Integer assessmentId = assessmentContext.getVeteranAssessmentId();
		VeteranAssessment veteranAssessment = veteranAssessmentRepository.findOne(assessmentId);

		// update the status of the assessment to complete
		AssessmentStatus status = assessmentStatusRepository.findOne(AssessmentStatusEnum.COMPLETE.getAssessmentStatusId());
		veteranAssessment.setAssessmentStatus(status);
		veteranAssessmentRepository.update(veteranAssessment);

		// TODO: Currently only a Veteran can take the assessment, person type will need to be detected once a
		// user can take an assessment to properly track the person_id
		veteranAssessment = veteranAssessmentRepository.findOne(assessmentId);
		VeteranAssessmentAuditLog auditLogEntry = VeteranAssessmentAuditLogHelper.createAuditLogEntry(veteranAssessment, ASSESSMENT_EVENT_MARKED_COMPLETED, veteranAssessment.getAssessmentStatus().getAssessmentStatusId(), PERSON_TYPE_VETERAN);
		veteranAssessmentAuditLogRepository.update(auditLogEntry);
	}
}
