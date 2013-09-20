App.System= DS.Model.extend({
  type: DS.attr('string'),
  env: DS.attr('string')
});

App.Machine= DS.Model.extend({
  cpus: DS.attr('number'),
  memory: DS.attr('number')
});
