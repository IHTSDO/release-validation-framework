// Allow for nested REST resources when creating, updating and deleting entities with Ember Data:
// by overriding RESTAdapter CRUD methods, making the record available to buildURL() and using the record's parents.
DS.RESTAdapter.reopen({
	createRecord: function(store, type, record) {
		return this.keepCurrentRecordAndCallSuper(store, type, record);
	},
	updateRecord: function(store, type, record) {
		return this.keepCurrentRecordAndCallSuper(store, type, record);
	},
	deleteRecord: function(store, type, record) {
		return this.keepCurrentRecordAndCallSuper(store, type, record);
	},
	keepCurrentRecordAndCallSuper: function(store, type, record) {
		// Keep hold of current record for URL building.
		this.setCurrentRecord(record);
		try {
			// Call original implementation
			return this._super(store, type, record);
		} finally {
			this.clearCurrentRecord();
		}
	},
	buildURL: function(type, id) {
		var currentRecord = this.getCurrentRecordAndClear();
		if (currentRecord) {
			return this.buildNestedURL(currentRecord);
		} else {
			return this._super(type, id);
		}
	},
	buildNestedURL: function(record) {
		var url;
		var type = record.constructor.typeKey;
		var id = get(record, 'id');

		var parent = record.get('parent');
		if (parent) {
			var urlParts = [];

			urlParts.push(this.buildNestedURL(parent));

			urlParts.push(this.pathForType(type));
			if (id) { urlParts.push(id); }

			url = urlParts.join('/');
		} else {
			url = this.buildURL(type, id);
		}
		return url;
	},
	setCurrentRecord: function(record) {
		this.currentRecord = record;
	},
	getCurrentRecordAndClear: function() {
		var currentRecord = this.currentRecord;
		this.clearCurrentRecord();
		return  currentRecord;
	},
	clearCurrentRecord: function() {
		delete this.currentRecord;
	}
});
