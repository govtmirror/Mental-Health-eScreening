(function() {
    'use strict';

    angular.module('Editors').controller('RulesDetailController',
    		['$scope', '$filter', '$state', '$q', 'rule', 'rules', 'MessageFactory', 'EventService', 'AssessmentVariableService',
     function($scope, $filter, $state, $q, rule, rules, MessageFactory, EventService, AssessmentVariableService) {

		var removeQueue = [], addQueue = [], newRule = true;

        $scope.rule = rule;
		$scope.selectedEvents = {};
		$scope.savedEvents = []; 
		$scope.assessmentVariables = [];
        $scope.consults = [];
        $scope.healthFactors = [];
        $scope.dashboardAlerts = [];
        $scope.questions = [];
		$scope.alerts = MessageFactory.get();
		$scope.showValidationMessages = false;
		$scope.enableTypeDropdown = false;

		if($scope.rule.id != null){
			newRule = false;
			rule.getList('events').then(function(events) {
				$scope.savedEvents = events;
				$scope.selectedEvents.consults = $filter('filter')(events, {type: 1});
				$scope.selectedEvents.healthFactors = $filter('filter')(events, {type: 2});
				$scope.selectedEvents.dashboardAlerts = $filter('filter')(events, {type: 3});
				$scope.selectedEvents.questions = $filter('filter')(events, {type: 4});
			});
		}
		
		AssessmentVariableService.query().then(function(avs){
			$scope.assessmentVariables = avs;
		});
		
		EventService.getList({type: 1}).then(function(consults){
			$scope.consults = consults;
		});
		
		
		EventService.getList({type: 2}).then(function(healthFactors){
			$scope.healthFactors = healthFactors;
		});
		
		
		EventService.getList({type: 3}).then(function(dashboardAlerts){
			$scope.dashboardAlerts = dashboardAlerts;
		});
		
		EventService.getList({type: 4}).then(function(questions){
			$scope.questions = questions;
		});
		

		$scope.removeEvent = function removeEvent(event) {
			removeQueue.push(event.id);
		};

		$scope.addEvent = function addEvent(event) {
			addQueue.push(event);
		};

		$scope.saveRule = function saveRule() {
			$scope.rule.save().then(function(response) {

				if($scope.rule.id == null){
					$scope.rule.id = response.id;
				}
				
				var eventUpdates = [];
				_.each(removeQueue, function(id) {
					var eventToDelete;
					$scope.savedEvents.some(function(event){ 
						if(id === event.id){
							eventToDelete = event; 
							return true;
						}
						return false;
					});
					
					if(eventToDelete){
						eventUpdates.push(
							eventToDelete.remove()
								.then(function(){
									//from removeQueue, remove the event ID of the removed event
									removeQueue = removeQueue.filter(function(eventId){return eventId != id});
						}));
					}
				});

				_.each(addQueue, function(event) {
					if($scope.savedEvents.every(function(savedEvent){ return savedEvent.id !== event.id; })){
						eventUpdates.push(
							$scope.rule.customPOST(event, 'events')
								.then(function(){
									//remove the event from addQueue after it is saved
									addQueue = addQueue.filter(function(addedEvent){return addedEvent.id !== event.id});
						}));
					}
				});

				if(eventUpdates.length == 0){
					successfulSave();
				}
				else{
					//track the results of all updates and trigger a screen update when they are done (or fail)
					$q.all(eventUpdates)
						.then(function(){
							successfulSave();
						},
						function(error){
							MessageFactory.error('There was a problem saving event updates. Please try again.');
						});
				}
				
			}, function(response) {
				MessageFactory.error('There was a problem saving the rule. Please try again.');
			});

		};
		
		$scope.cancel = function () {
			$state.go('rules');
		};
		
		function successfulSave(){
			if(newRule){ //add new rule to rule list
				rules.push($scope.rule);
			}
			else{ //update rule name in list 
				rules.some(function(r){
					if(r.id == $scope.rule.id){
						r.name = $scope.rule.name;
						return true;
					}
					return false;
				});
			}
			
			MessageFactory.add('success', "Rule '" + $scope.rule.name + "' was successfully saved.", true, true);
			$state.go('rules');
		};

    }]);
})();

