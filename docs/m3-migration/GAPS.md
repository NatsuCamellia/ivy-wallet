# Known gaps

Known gaps are listed here.

- Reorder with a list taller than the sheet (3.4a): `flow_reorder` runs on the two default accounts only, so scrolling and dragging inside a sheet whose rows overflow it is not covered by `m3-smoke.sh`. Onboarding offers no way to seed more than two accounts, and adding ten or more accounts through the UI would make the flow several minutes long. Needs a seeded database or a debug intent before it can be automated.
