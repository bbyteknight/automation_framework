-- Generic templated SELECT used by steps.DbSteps for every "table should contain a record
-- where ..." / "column should equal ..." assertion. ${table} and ${conditions} are substituted
-- by DbSteps after validating both are safe SQL identifiers (never raw user/Gherkin text) —
-- the actual compared values are always bound as real "?" JDBC parameters, never concatenated.
SELECT * FROM ${table} WHERE ${conditions}
