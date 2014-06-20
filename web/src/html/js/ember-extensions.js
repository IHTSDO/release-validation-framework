//
// Ember Extensions
//

// Set JQuery Validate defaults to match Bootstrap layout.
$.validator.setDefaults({
	highlight: function(element) {
		$(element).closest('.form-group').addClass('has-error');
	},
	unhighlight: function(element) {
		$(element).closest('.form-group').removeClass('has-error');
	},
	errorElement: 'span',
	errorClass: 'help-block',
	errorPlacement: function(error, element) {
		if(element.parent('.input-group').length) {
			error.insertAfter(element.parent());
		} else {
			error.insertAfter(element);
		}
	}
});

Ember.EasyForm.Config.registerWrapper('default', {
	inputTemplate: 'easyform-override/bootstrap-input',
	inputClass: 'form-group',
	hintClass: 'help-block',
	fieldErrorClass: 'has-error',
	errorClass: 'alert alert-danger'
});
