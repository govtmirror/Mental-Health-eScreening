/**
 * Represents the application api.  If the variable is already defined use it,
 * otherwise create an empty object.
 *
 * @type {EScreeningDashboardApp|*|EScreeningDashboardApp|*|{}|{}}
 */
var EScreeningDashboardApp = EScreeningDashboardApp || {};
/**
 * Represents the application static variable. Use existing static variable, if one already exists,
 * otherwise create a new one.
 *
 * @static
 * @type {*|EScreeningDashboardApp.models|*|EScreeningDashboardApp.models|Object|*|Object|*}
 */
EScreeningDashboardApp.models = EScreeningDashboardApp.models || EScreeningDashboardApp.namespace("gov.va.escreening.models");
/**
 * Constructor method for the Template class.  The properties of this class can be initialized with
 * the jsonTemplateObject.  
 * Please Note: This is used for both instantiating new objects and for extending Restangular models.  Because of this there cannot 
 * be any fields defined unless we are creating a new instance.  Currently if we are creating an instance the templateCondig must be
 * given with the type of the template so we can save it. When that happens the fields will have default values set. 
 *
 * @class
 * @classdesc   This class is a domain model class; which means it has both behavior and state
 *              information about Template Types.
 * @param {JSON}  templateConfig  Represents the JSON representation of a Template object.
 * @constructor
 * @author Robin Carnow
 */
EScreeningDashboardApp.models.Template = function (templateConfig) {


    if(Object.isDefined(templateConfig)){
        this.id = Object.isDefined(templateConfig.id)? templateConfig.id : null;
        this.isGraphical = (Object.isBoolean(templateConfig.isGraphical))? templateConfig.isGraphical : false;
        this.type = (Object.isDefined(templateConfig.type))? templateConfig.type: null;
        this.name = (Object.isDefined(templateConfig.type) && Object.isDefined(templateConfig.type.name))? templateConfig.type.name: null;
        this.blocks = (Object.isArray(templateConfig.blocks))? EScreeningDashboardApp.models.TemplateBlock.createTemplateBlockArray(templateConfig.blocks): [];
        
    }
    
    /**
     * Runs any initialization tasks after a model extend
     */
    this.init = function(){
        if(Object.isArray(this.blocks)){
            this.blocks = EScreeningDashboardApp.models.TemplateBlock.createTemplateBlockArray(this.blocks);
        }
    };
    
    /**
     * Because of some of the awkward nature of our endpoints and pre-restangular, UI code we have to do this for now
     * @param related object is the object which is associated with the template being saved. The related object 
     * can be either a battery or a survey.  The related object parameter should look like this:
     * {
     *   id : [surveyId | batteryId],
     *   name : the display name of the related object,
     *   type : ["module" | "battery"]
     * }
     * 
     */
    this.saveFor = function(relatedObject){
        var isPostable = Object.isDefined(this.type.customPOST);
        
        if(Object.isDefined(this.id) 
                && this.id != -1
                && (parseInt(this.id) == this.id || this.id.length > 0)){
            //this is an update
            return this.put();
        }
        else if(isPostable && relatedObject.type == "module"){ //this is a new template
            console.log("Saving new template of type " + this.type.id  + " for survey with ID: " + relatedObject.id)
            //TODO: if/when we move to a more OO restful end point structure we should remove this in favor of:
            // var templateTypes = module.getList('templateTypes');
            // ... at some point: new EScreeningDashboardApp.models.Template({type: selectedTemplateType});
            // this.type.post(this);  
            // The goal would be to have "this" sent to either:
            //  -  POST: /services/surveys/{surveyId}/templateTypes/{templateTypeId}
            //  or this:
            //  -  POST: /services/batteries/{batteryId}/templateTypes/{templateTypeId}
            return this.type.customPOST(this, "surveys/" + relatedObject.id);
        }
        else if(isPostable && relatedObject.type == "battery"){ //this is a new template
            console.log("Saving new template of type " + this.type.id + " for battery with ID: " + relatedObject.id);
            return this.type.customPOST(this, "batteries/" + relatedObject.id);
        }
        
        var logMsg = "State was lost for the currently edited template.  No ID was found but it is also not a new template.";
        var userMsg = "Editor state was lost. Call support.";
        if(isPostable){
            logMsg = "Unsupported operation. Template is new and related object is unsupported: " + JSON.stringify(relatedObject);
            userMsg = "Unsupported operation. Call support.";
        }
        console.error(logMsg);
        var deferred = $q.defer();
        deferred.reject(userMsg);
        return deferred.promise;
    };
    
    /**
     * Updates the section numbering of all blocks as well as each block's parent field
     */
    this.updateSections = function(){
        
        function updateSection(children, parent){
            if(Object.isDefined(children)){
                for (var i = 0; i < children.length; i++){
                    var sectionIndex = i + 1;
                    children[i].setParent(parent);
                    children[i].section = parent ? parent.section + "." + sectionIndex : "" + sectionIndex;
                    updateSection(children[i].children, children[i]);
                }
            }
        }
        return updateSection(this.blocks);
    };
    
    this.toString = function () {
        return "Template {id: " + this.id + ", type: " + this.type + ", isGraphical: " + this.isGraphical + ", with " + blocks.length + " blocks.}";
    };

};