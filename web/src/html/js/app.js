App = Ember.Application.create();

App.ApplicationController = Ember.ObjectController.extend({
    routeChanged: function() {
        // Scroll top top of new page
        window.scrollTo(0, 0);

        //Initialise popovers for all elements that include the relevant attribute
        //Needs to be repeated each time the DOM changes
        //Ember.run.scheduleOnce('afterRender', this, afterRender);
        Ember.run.later(window, afterRender, 1000);
    }.observes('currentPath')
});

App.Router.map(function() {
    // put your routes here
    this.resource('assertions', function() {
        this.resource('assertion', {path: 'view/:assertion_id'});
        this.resource('edit-assertion', {path: 'edit/:assertion_id'});
    });
    this.resource('tests');
    this.resource('create-assertion');

});

App.AbstractController = Ember.ObjectController.reopen({
    needs: "application"
});

App.ApplicationRoute = Ember.Route.extend({
    beforeModel: function() {
        App.store = this.store;
    },
    actions: {
        removeEntity: function(model) {
            console.log('removeEntity', model);
            this.send('openModal', 'remove-entity', model);
        },
        openModal: function(modalName, params) {
            console.log('openModal for modal with name: ' + modalName);
            var controller = this.controllerFor(modalName);
            var model = null;
            if (controller.getModel) {
                model = controller.getModel(params);
            } else {
                model = params;
            }
            controller.set('model', model);
            return this.render(modalName, {
                into: 'application',
                outlet: 'modal',
                controller: controller
            });
        },
        modalInserted: function() {
            var $modal = $('.modal');
            var route = this;
            $modal.on('hidden.bs.modal', function(e) {
                route.disconnectOutlet({
                    outlet: 'modal',
                    parentView: 'application'
                });
            });
            $modal.modal('show');
        },
        closeModal: function() {
            $('.modal').modal('hide');
        }
    }
});

// assertions
App.AssertionsRoute = Ember.Route.extend({
    model : function() {
        return this.store.findAll('assertion');
    },
    actions: {
        addAssertion: function() {
            this.send('openModal', 'create-assertion');
        }
    }
});

App.AssertionRoute = Ember.Route.extend({
    model : function(params) {
        return this.store.find('assertion', params.assertion_id);
    }
});

App.EditAssertionRoute = Ember.Route.extend({
    model : function(params) {
        return this.store.find('assertion', params.assertion_id);
    }
});

// add assertion
App.CreateAssertionView = Ember.View.extend({
    templateName: 'create-assertion',
    didInsertElement: function() {
        this.controller.send('modalInserted');
    }
});

App.CreateAssertionController = Ember.ObjectController.extend({
    getModel: function() {
        var assertion = this.store.createRecord('assertion');
        return  assertion;
    },
    actions: {
        submit: function() {
            var assertion = this.get('model');
            var nameVal = assertion.get('name');
            if(!nameVal || nameVal.trim().length < 3) {
                $('#c_ass_name_error').show();
            } else {
                $('#c_ass_name_error').hide();
                assertion.save();
                this.send('closeModal');
                this.transitionToRoute('assertions');
            }
        },
        cancel: function() {
            var assertion = this.get('model');
            assertion.deleteRecord();
            this.send('closeModal');
        },
        close: function() {
            var assertion = this.get('model');
            assertion.deleteRecord();
            this.send('closeModal');
        }
    }
});


App.EditAssertionView = Ember.View.extend({
    templateName: 'edit-assertion'
});

App.EditAssertionController = Ember.ObjectController.extend({
    getModel : function(params) {
        return this.store.find('assertion', params.assertion_id);
    },
    actions: {
        submit: function() {
            var model = this.get('model');
            console.log("2 ", model);
            //this.store.push('assertion', assertion);
            model.save();
            this.transitionToRoute('assertions');
        },
        cancel: function() {
            var assertion = this.get('model');
            console.log("Dont like ember", assertion);
            assertion.rollback();
            this.transitionToRoute('assertions');
        }
    }
});
// tests

// Confirm dialog
App.ConfirmDialogView = Ember.View.extend({
	templateName: 'confirm-dialog',
	didInsertElement: function() {
		this.controller.send('modalInserted');
	}
});
App.ConfirmDialogController = Ember.ObjectController.extend({
	actions: {
		proceed: function() {
			var model = this.get('model');
			var guardedFunction = model.guardedFunction;
			guardedFunction();
		}
	}
});


function afterRender() {
	console.log("in function afterRender()");
	$("[data-toggle='tooltip']").tooltip();
	effectPulse($('.traffic-light-in-progress'));
	//initUploadForm();
}

function effectPulse($selection) {

	if ($selection && $selection.size() > 0) {
		$selection.stop();
		$selection.clearQueue();
		$selection.animate({
			opacity: 0
		}, 900, function() {
			$selection.animate({
				opacity: 1
			}, 900, function() {
				effectPulse($('.traffic-light-in-progress'));
			})
		})
	}
}
