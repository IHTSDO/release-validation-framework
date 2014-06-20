App.DSModel = DS.Model.extend(Ember.Validations.Mixin);

// Models

//App.Center = App.DSModel.extend({
//
//	name: DS.attr(),
//	shortName: DS.attr(),
//	inactivated: DS.attr('boolean'),
//	validations: {
//		name: {
//			presence: true,
//			length: { minimum: 3 }
//		},
//		shortName: {
//			presence: true,
//			length: { minimum: 3 }
//		}
//	}
//});

App.Assertion = DS.Model.extend({
	name: DS.attr(),
	description: DS.attr(),
	statement: DS.attr(),
	docLink: DS.attr(),
	keywords: DS.attr(),
	tests: DS.hasMany('test', { async: true }),
    validations: {
        name: {
            presence: true,
            length: { minimum: 3 }

        }
    }
});

App.Test = DS.Model.extend({
	parent: DS.belongsTo('assertion'),
	name: DS.attr()
});


//App.ExecutionConfiguration = DS.Model.extend({
//	dummy: DS.attr(),
//	json: function() {
//		return JSON.stringify(this._data, null, 2);
//	}.property('dummy')
//});
// Configuration

App.namespace = 'api/v1';

// Configure REST location
DS.RESTAdapter.reopen({
	namespace: App.namespace,
	pathForType: function(type) {
		return this._super(type);
	}
});
