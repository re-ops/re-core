App.Adapter = DS.RESTAdapter.extend({
  bulkCommit: false, 
  host: 'https://localhost:8443', 
});

App.Store = DS.Store.extend({
  revision: 12,
  adapter:  App.Adapter.create()
});
