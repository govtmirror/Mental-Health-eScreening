package gov.va.escreening.controller.dashboard;

import gov.va.escreening.delegate.CreateAssessmentDelegate;
import gov.va.escreening.domain.AssessmentStatusEnum;
import gov.va.escreening.domain.BatteryDto;
import gov.va.escreening.domain.BatterySurveyDto;
import gov.va.escreening.domain.SurveyDto;
import gov.va.escreening.domain.VeteranDto;
import gov.va.escreening.dto.DropDownObject;
import gov.va.escreening.entity.Program;
import gov.va.escreening.entity.User;
import gov.va.escreening.entity.VeteranAssessment;
import gov.va.escreening.form.EditVeteranAssessmentFormBean;
import gov.va.escreening.repository.UserRepository;
import gov.va.escreening.security.CurrentUser;
import gov.va.escreening.security.EscreenUser;
import gov.va.escreening.service.AssessmentAlreadyExistException;
import gov.va.escreening.service.UserService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("editVeteranAssessmentController")
@RequestMapping(value = "/dashboard")
public class EditVeteranAssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(EditVeteranAssessmentController.class);
    @Resource(name = "userDetailsService")
    UserDetailsService userDetailsService;
    private CreateAssessmentDelegate createAssessmentDelegate;
    private UserService userService;

    public EditVeteranAssessmentController() {
        // Default constructor.
    }

    @Autowired
    public void setCreateAssessmentDelegate(
            CreateAssessmentDelegate createAssessmentDelegate) {
        this.createAssessmentDelegate = createAssessmentDelegate;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the backing bean for the form.
     *
     * @return
     */
    @ModelAttribute
    public EditVeteranAssessmentFormBean getEditVeteranAssessmentFormBean() {
        logger.trace("Creating new EditVeteranAssessmentFormBean");
        return new EditVeteranAssessmentFormBean();
    }

    /**
     * Called when the Create Battery page is opened (new or not)
     *
     * @param model
     * @param editVeteranAssessmentFormBean
     * @param veteranAssessmentId
     * @param veteranId
     * @param escreenUser
     * @return
     */
    @RequestMapping(value = "/editVeteranAssessment", method = RequestMethod.GET)
    public String setupPage(
            Model model,
            @ModelAttribute EditVeteranAssessmentFormBean editVeteranAssessmentFormBean,
            @RequestParam(value = "vaid", required = false) Integer veteranAssessmentId,
            @RequestParam(value = "vid", required = false) Integer veteranId,
            @RequestParam(value = "clinicianId", required = false) Integer clinicianId,
            @RequestParam(value = "clinicId", required = false) Integer clinicId,
            @RequestParam(value = "noteTitleId", required = false) Integer noteTitleId,
            @RequestParam(value = "programId", required = false) Integer programId,
            @CurrentUser EscreenUser escreenUser) {

        logger.trace("Using the assessment dashboard mapping");

        if (veteranId == null && veteranAssessmentId == null) {
            throw new IllegalArgumentException("Both Veteran Assessment ID and Veteran ID are missing.");
        }

        boolean isCreateMode = false;
        boolean isReadOnly = false;
        String veteranAssessmentStatus = AssessmentStatusEnum.CLEAN.name();
        Date dateCreated = null;
        Date dateCompleted = null;

        // Determine if we should preselect modules.
        if (veteranAssessmentId == null) {
            isCreateMode = true;
            isReadOnly = false;
            veteranAssessmentStatus = StringUtils.capitalize(AssessmentStatusEnum.CLEAN.name().toLowerCase());
        }

        // 1. Get the veteran assessment
        VeteranAssessment veteranAssessment = null;

        if (veteranAssessmentId != null) {
            veteranAssessment = createAssessmentDelegate.getVeteranAssessmentByVeteranAssessmentId(veteranAssessmentId);

            if (veteranAssessment == null) {
                throw new IllegalArgumentException("Veteran Assessment is null: " + veteranAssessmentId);
            }

            isReadOnly = createAssessmentDelegate.isVeteranAssessmentReadOnly(veteranAssessmentId);
            veteranAssessmentStatus = veteranAssessment.getAssessmentStatus().getName();
            dateCreated = veteranAssessment.getDateCreated();
            dateCompleted = veteranAssessment.getDateCompleted();

            veteranId = veteranAssessment.getVeteran().getVeteranId();
        }

        model.addAttribute("isCprsVerified", escreenUser.getCprsVerified());
        model.addAttribute("isCreateMode", isCreateMode);
        model.addAttribute("isReadOnly", isReadOnly);
        model.addAttribute("dateCreated", dateCreated);
        model.addAttribute("dateCompleted", dateCompleted);
        model.addAttribute("veteranAssessmentStatus", veteranAssessmentStatus);

        // Set these properties to be used during postback.
        editVeteranAssessmentFormBean.setVeteranAssessmentId(veteranAssessmentId);
        editVeteranAssessmentFormBean.setVeteranId(veteranId);

        // 2. Get veteran
        VeteranDto veteranDto = createAssessmentDelegate.getVeteranFromDatabase(veteranId);
        model.addAttribute("veteran", veteranDto);

        // 4. Get all programs.
        List<DropDownObject> programList = createAssessmentDelegate.getProgramList(escreenUser.getProgramIdList());
        model.addAttribute("programList", programList);

//        // 5. Get all battery list.
//        List<DropDownObject> batteryList = createAssessmentDelegate.getBatteryList();
//        model.addAttribute("batteryList", batteryList);
//
//        Map<String, String> programsMap = createProgramsMap(batteryList);
//        model.addAttribute("programsMap", programsMap);

        // 6. Get all battery survey list.
        List<BatterySurveyDto> batterySurveyList = createAssessmentDelegate.getBatterySurveyList();
        model.addAttribute("batterySurveyList", batterySurveyList);

        // 7. Get all the modules (surveys) that can be assigned
        List<SurveyDto> surveyList = isCreateMode ? createAssessmentDelegate.getSurveyList() : createAssessmentDelegate.getSurveyListUnionAssessment(veteranAssessmentId);

        // 8. Populate survey list with list of batteries it is associated with
        // to make it easier in view.
        for (BatterySurveyDto batterySurvey : batterySurveyList) {

            BatteryDto batteryDto = new BatteryDto(batterySurvey.getBatteryId(), batterySurvey.getBatteryName());

            for (SurveyDto survey : surveyList) {
                if (survey.getSurveyId().intValue() == batterySurvey.getSurveyId().intValue()) {
                    if (survey.getBatteryList() == null) {
                        survey.setBatteryList(new ArrayList<BatteryDto>());
                    }

                    survey.getBatteryList().add(batteryDto);
                    break;
                }
            }
        }
        model.addAttribute("surveyList", surveyList);

        // 9. Get selected program
        if (programId != null || (veteranAssessment != null && veteranAssessment.getProgram() != null)) {
            Integer pid = programId != null ? programId : veteranAssessment.getProgram().getProgramId();
            editVeteranAssessmentFormBean.setSelectedProgramId(pid);

            List<DropDownObject> batteryList = createAssessmentDelegate.getBatteryListByProgram(pid);
            model.addAttribute("batteryList", batteryList);

            // Get all clinic list since we have a program.
            List<DropDownObject> clinicList = createAssessmentDelegate.getClinicList(pid);
            model.addAttribute("clinicList", clinicList);
            editVeteranAssessmentFormBean.setSelectedClinicId(clinicId);

            List<DropDownObject> noteTitleList = createAssessmentDelegate.getNoteTitleList(pid);
            model.addAttribute("noteTitleList", noteTitleList);
            editVeteranAssessmentFormBean.setSelectedNoteTitleId(noteTitleId);

            // Get all clinician list since we have a clinic.
            List<DropDownObject> clinicianList = createAssessmentDelegate.getClinicianList(pid);
            model.addAttribute("clinicianList", clinicianList);
            editVeteranAssessmentFormBean.setSelectedClinicianId(clinicianId);
        }

        // 10. Get selected clinic
        if (veteranAssessment != null && veteranAssessment.getClinic() != null) {
            editVeteranAssessmentFormBean.setSelectedClinicId(veteranAssessment.getClinic().getClinicId());
        }

        // 11. Get selected clinician
        if (veteranAssessment != null && veteranAssessment.getClinician() != null) {
            editVeteranAssessmentFormBean.setSelectedClinicianId(veteranAssessment.getClinician().getUserId());
        }

        // 12. Get selected note title
        if (veteranAssessment != null && veteranAssessment.getNoteTitle() != null) {
            editVeteranAssessmentFormBean.setSelectedNoteTitleId(veteranAssessment.getNoteTitle().getNoteTitleId());
        }

        // 13. Get selected battery
        if (veteranAssessment != null && veteranAssessment.getBattery() != null) {
            editVeteranAssessmentFormBean.setSelectedBatteryId(veteranAssessment.getBattery().getBatteryId());
        }

        // 14. Get the full name of the created by user.
        if (veteranAssessment != null && veteranAssessment.getCreatedByUser() != null) {
            model.addAttribute("createdByUser", userService.getFullName(veteranAssessment.getCreatedByUser()));
        }

        // 15. Get all the surveys already assigned to this veteran assessment.
        List<SurveyDto> veteranAssessmentSurveyList = null;

        if (!isCreateMode) {
            veteranAssessmentSurveyList = createAssessmentDelegate.getVeteranAssessmentSurveyList(veteranAssessmentId);
        }

        if (veteranAssessmentSurveyList != null && veteranAssessmentSurveyList.size() > 0) {

            for (SurveyDto survey : veteranAssessmentSurveyList) {
                editVeteranAssessmentFormBean.getSelectedSurveyIdList().add(survey.getSurveyId());
            }
        }

        // 14. If the veteran has already been mapped to a VistA record, then we
        // can look up clinical reminders for the
        // veteran. This will pre-select or auto assign modules (surveys).
        // the eScreenUser will be changed from the user who is logged in to the clinician. The Notes will be pulled for the clinician

        EscreenUser clinicianUser = clinicianId != null ? findEscreenUser(clinicianId) : escreenUser;
        if (clinicianUser.getCprsVerified() && StringUtils.isNotBlank(veteranDto.getVeteranIen())) {

            Map<Integer, String> autoAssignedSurveyMap = createAssessmentDelegate.getPreSelectedSurveyMap(clinicianUser, veteranDto.getVeteranIen());

            // For each survey, pre-select it and also indicate reason in the
            // note.
            if (autoAssignedSurveyMap != null && !autoAssignedSurveyMap.isEmpty()) {
                for (int i = 0; i < surveyList.size(); ++i) {
                    Integer surveyId = surveyList.get(i).getSurveyId();

                    // Preselect it and populate note field.
                    if (autoAssignedSurveyMap.containsKey(surveyId)) {

                        // Only auto assign for 'create mode'.
                        if (isCreateMode) {
                            if (!editVeteranAssessmentFormBean.getSelectedSurveyIdList().contains(surveyId)) {
                                editVeteranAssessmentFormBean.getSelectedSurveyIdList().add(surveyId);
                            }
                        }

                        surveyList.get(i).setNote(autoAssignedSurveyMap.get(surveyId));
                    }
                }
            }
        }

        return programId != null ? "dashboard/editVeteranAssessmentTail" : "dashboard/editVeteranAssessment";
    }

    public EscreenUser findEscreenUser(Integer clinicianId) {
        User user = userService.findUser(clinicianId);
        return (EscreenUser) userDetailsService.loadUserByUsername(user.getLoginId());
    }

    private Map<String, String> createProgramsMap(
            List<DropDownObject> batteryList) {
        Map<String, String> pm = new HashMap<String, String>();

        for (DropDownObject b : batteryList) {
            List<Program> ps = createAssessmentDelegate.getProgramsForBattery(Integer.parseInt(b.getStateId()));
            StringBuilder sb = new StringBuilder();
            for (Program p : ps) {
                sb.append("program_" + p.getProgramId()).append(" ");
            }
            pm.put(b.getStateId(), sb.toString());
        }
        return pm;
    }

    @RequestMapping(value = "/editVeteranAssessment", method = RequestMethod.POST, params = "saveButton")
    public String processPage(
            Model model,
            @Valid @ModelAttribute EditVeteranAssessmentFormBean editVeteranAssessmentFormBean,
            BindingResult result,
            // RedirectAttributes redirectAttributes,
            @RequestParam(value = "vaid", required = false) Integer veteranAssessmentId,
            @RequestParam(value = "vid", required = false) Integer veteranId,
            @CurrentUser EscreenUser escreenUser) {

        logger.trace(editVeteranAssessmentFormBean.toString());

        // First check if the Veteran stated taking the assessment while the
        // staff member was trying to edit the data.
        if (editVeteranAssessmentFormBean.getSelectedProgramId() != null) {

            boolean isReadOnly = createAssessmentDelegate.isVeteranAssessmentReadOnly(editVeteranAssessmentFormBean.getVeteranAssessmentId());

            if (isReadOnly) {
                result.reject("dashboard.createBattery.nolongereditable", "The veteran has started taking the Battery, and this Battery is no longer editable.");
            }
        }

        // If there is an error, return the same view.
        if (result.hasErrors()) {

            if (veteranId == null && veteranAssessmentId == null) {
                throw new IllegalArgumentException("Both Veteran Assessment ID and Veteran ID are missing.");
            }

            boolean isCreateMode = false;
            boolean isReadOnly = false;
            String veteranAssessmentStatus = AssessmentStatusEnum.CLEAN.name();
            Date dateCreated = null;
            Date dateCompleted = null;

            // Determine if we should preselect modules.
            if (veteranAssessmentId == null) {
                isCreateMode = true;
                isReadOnly = false;
                veteranAssessmentStatus = StringUtils.capitalize(AssessmentStatusEnum.CLEAN.name().toLowerCase());
            }

            // 1. Get the veteran assessment
            VeteranAssessment veteranAssessment = null;

            if (veteranAssessmentId != null) {
                veteranAssessment = createAssessmentDelegate.getVeteranAssessmentByVeteranAssessmentId(veteranAssessmentId);

                if (veteranAssessment == null) {
                    throw new IllegalArgumentException("Veteran Assessment is null: " + veteranAssessmentId);
                }

                isReadOnly = createAssessmentDelegate.isVeteranAssessmentReadOnly(veteranAssessmentId);
                veteranAssessmentStatus = veteranAssessment.getAssessmentStatus().getName();
                dateCreated = veteranAssessment.getDateCreated();
                dateCompleted = veteranAssessment.getDateCompleted();

                veteranId = veteranAssessment.getVeteran().getVeteranId();
            }

            resetPage(model, editVeteranAssessmentFormBean, veteranId,
                    escreenUser, isCreateMode, isReadOnly,
                    veteranAssessmentStatus, dateCreated, dateCompleted,
                    veteranAssessment);

            return "dashboard/editVeteranAssessment";
        }

        if (editVeteranAssessmentFormBean.getVeteranAssessmentId() != null) {

            boolean isReadOnly = createAssessmentDelegate.isVeteranAssessmentReadOnly(editVeteranAssessmentFormBean.getVeteranAssessmentId());

            if (isReadOnly) {
                throw new IllegalArgumentException("Veteran Assessment is in a Read Only state but somehow tried to edit : " + editVeteranAssessmentFormBean.getVeteranAssessmentId());
            }

            // Edit
            createAssessmentDelegate.editVeteranAssessment(escreenUser, editVeteranAssessmentFormBean.getVeteranAssessmentId(), editVeteranAssessmentFormBean.getSelectedProgramId(), editVeteranAssessmentFormBean.getSelectedClinicId(), editVeteranAssessmentFormBean.getSelectedClinicianId(), editVeteranAssessmentFormBean.getSelectedNoteTitleId(), editVeteranAssessmentFormBean.getSelectedBatteryId(), editVeteranAssessmentFormBean.getSelectedSurveyIdList());

            model.addAttribute("vid", editVeteranAssessmentFormBean.getVeteranId());
            return "redirect:/dashboard/veteranDetail";
        } else {
            // Add
            try {
                createAssessmentDelegate.createVeteranAssessment(escreenUser, editVeteranAssessmentFormBean.getVeteranId(), editVeteranAssessmentFormBean.getSelectedProgramId(), editVeteranAssessmentFormBean.getSelectedClinicId(), editVeteranAssessmentFormBean.getSelectedClinicianId(), editVeteranAssessmentFormBean.getSelectedNoteTitleId(), editVeteranAssessmentFormBean.getSelectedBatteryId(), editVeteranAssessmentFormBean.getSelectedSurveyIdList());

                model.addAttribute("vid", editVeteranAssessmentFormBean.getVeteranId());
                model.addAttribute("ibc", true);
                return "redirect:/dashboard/veteranDetail";
            } catch (AssessmentAlreadyExistException ex) {
                result.reject("dashboard.createBattery.alreadyExist", "The Veteran already has an assessment with this program");

                resetPage(model, editVeteranAssessmentFormBean, veteranId, escreenUser, true, false,
                        StringUtils.capitalize(AssessmentStatusEnum.CLEAN.name().toLowerCase()),
                        null, null, null);

                return "dashboard/editVeteranAssessment";
            }
        }
    }

    public void resetPage(Model model,
                          EditVeteranAssessmentFormBean editVeteranAssessmentFormBean,
                          Integer veteranId, EscreenUser escreenUser, boolean isCreateMode,
                          boolean isReadOnly, String veteranAssessmentStatus,
                          Date dateCreated, Date dateCompleted,
                          VeteranAssessment veteranAssessment) {
        model.addAttribute("isCprsVerified", escreenUser.getCprsVerified());
        model.addAttribute("isCreateMode", isCreateMode);
        model.addAttribute("isReadOnly", isReadOnly);
        model.addAttribute("dateCreated", dateCreated);
        model.addAttribute("dateCompleted", dateCompleted);
        model.addAttribute("veteranAssessmentStatus", veteranAssessmentStatus);

        // Set these properties to be used during postback.
        // editVeteranAssessmentFormBean.setVeteranAssessmentId(veteranAssessmentId);
        // editVeteranAssessmentFormBean.setVeteranId(veteranId);

        // 2. Get veteran
        VeteranDto veteranDto = createAssessmentDelegate.getVeteranFromDatabase(veteranId);
        model.addAttribute("veteran", veteranDto);

        // 4. Get all programs.
        List<DropDownObject> programList = createAssessmentDelegate.getProgramList(escreenUser.getProgramIdList());
        model.addAttribute("programList", programList);

        // 5. Get all battery list.
        List<DropDownObject> batteryList = createAssessmentDelegate.getBatteryList();
        model.addAttribute("batteryList", batteryList);

        Map<String, String> programsMap = createProgramsMap(batteryList);
        model.addAttribute("programsMap", programsMap);

        // 6. Get all battery survey list.
        List<BatterySurveyDto> batterySurveyList = createAssessmentDelegate.getBatterySurveyList();
        model.addAttribute("batterySurveyList", batterySurveyList);

        // 7. Get all the modules (surveys) that can be assigned
        List<SurveyDto> surveyList = createAssessmentDelegate.getSurveyList();

        // 8. Populate survey list with list of batteries it is associated
        // with to make it easier in view.
        for (BatterySurveyDto batterySurvey : batterySurveyList) {

            BatteryDto batteryDto = new BatteryDto(batterySurvey.getBatteryId(), batterySurvey.getBatteryName());

            for (SurveyDto survey : surveyList) {
                if (survey.getSurveyId().intValue() == batterySurvey.getSurveyId().intValue()) {
                    if (survey.getBatteryList() == null) {
                        survey.setBatteryList(new ArrayList<BatteryDto>());
                    }

                    survey.getBatteryList().add(batteryDto);
                    break;
                }
            }
        }
        model.addAttribute("surveyList", surveyList);

        // 9. Get selected program
        if (editVeteranAssessmentFormBean.getSelectedProgramId() != null) {

            // Get all clinic list since we have a program.
            List<DropDownObject> clinicList = createAssessmentDelegate.getClinicList(editVeteranAssessmentFormBean.getSelectedProgramId());
            model.addAttribute("clinicList", clinicList);

            List<DropDownObject> noteTitleList = createAssessmentDelegate.getNoteTitleList(editVeteranAssessmentFormBean.getSelectedProgramId());
            model.addAttribute("noteTitleList", noteTitleList);

            // Get all clinician list since we have a clinic.
            List<DropDownObject> clinicianList = createAssessmentDelegate.getClinicianList(editVeteranAssessmentFormBean.getSelectedProgramId());
            model.addAttribute("clinicianList", clinicianList);
        }

        // 14. Get the full name of the created by user.
        if (veteranAssessment != null && veteranAssessment.getCreatedByUser() != null) {
            model.addAttribute("createdByUser", userService.getFullName(veteranAssessment.getCreatedByUser()));
        }

        // 15. If the veteran has already been mapped to a VistA record,
        // then we can look up clinical reminders for
        // the veteran. This will pre-select or auto assign modules
        // (surveys).
        if (escreenUser.getCprsVerified() && StringUtils.isNotBlank(veteranDto.getVeteranIen())) {

            Map<Integer, String> autoAssignedSurveyMap = createAssessmentDelegate.getPreSelectedSurveyMap(escreenUser, veteranDto.getVeteranIen());

            // For each survey, pre-select it and also indicate reason in
            // the note.
            if (autoAssignedSurveyMap != null && !autoAssignedSurveyMap.isEmpty()) {
                for (int i = 0; i < surveyList.size(); ++i) {
                    Integer surveyId = surveyList.get(i).getSurveyId();

                    // Preselect it and populate note field.
                    if (autoAssignedSurveyMap.containsKey(surveyId)) {
                        surveyList.get(i).setNote(autoAssignedSurveyMap.get(surveyId));
                    }
                }
            }
        }
    }

    @RequestMapping(value = "/editVeteranAssessment", method = RequestMethod.POST, params = "cancelButton")
    public String processCancelPage(
            Model model,
            @ModelAttribute EditVeteranAssessmentFormBean editVeteranAssessmentFormBean,
            @CurrentUser EscreenUser escreenUser) {

        logger.trace(editVeteranAssessmentFormBean.toString());

        model.addAttribute("vid", editVeteranAssessmentFormBean.getVeteranId());
        return "redirect:/dashboard/veteranDetail";
    }

}
