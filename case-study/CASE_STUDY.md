# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
```txt
The core challenge is that costs rarely map 1:1 onto the entities this system already models
(Warehouse, Store, Product). Labor and overhead are usually incurred at the Warehouse level as a
whole, not per Product, so allocating them down to a Product/Store fulfillment relationship
requires a chosen allocation basis (per unit shipped, per storage slot occupied, per pick/pack
event) - and whichever basis is picked will bias the numbers in some direction. Transportation cost
is worse still, since it's typically incurred per shipment/route and may span multiple Stores and
Products in one trip, so it needs its own apportionment rule rather than a natural join. The
Warehouse-to-Product-to-Store fulfillment relationship this assignment introduces (the bonus task)
is actually the natural anchor point for this: once you know which Warehouse fulfilled which
Product for which Store, and in what volume, you have a defensible basis for allocating shared
costs proportionally - but only if that data is captured at the time of the transaction, not
reconstructed after the fact.

Key considerations:
- Direct vs. shared costs: some costs (a Store's own local labor) are directly attributable; others
  (a Warehouse's rent, a regional trucking contract) are shared and need an explicit, documented
  allocation method - and that method itself needs to be a first-class, versioned piece of data
  (see Scenario 5), not a spreadsheet formula that changes silently.
- Timing/granularity mismatch: labor and overhead are usually known monthly, while
  inventory/transportation costs can be known per-transaction. Mixing granularities without care
  produces numbers that look precise but aren't.
- Warehouse replacement (Scenario 5) directly affects this: if a Warehouse is replaced mid-period,
  costs need to be split correctly between the old and new Warehouse rows rather than either
  double-counted or lost, which argues for cost data being tied to the specific Warehouse row (by
  its database identity, not just its reused Business Unit Code) - the same design choice this
  assignment's Fulfillment feature made deliberately, for the same reason.

Questions I'd want answered before scoping this work: What's the finest granularity the business
actually needs to act on (per-Store P&L? per-Product margin? just per-Warehouse overhead)? Which
costs are already captured in a source system (payroll, TMS, WMS) versus which would need net-new
capture in this system? Is the allocation methodology itself something Finance already has a policy
for, or is defining it part of this project's scope?
```

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
```txt
With the entities in this system, a few concrete levers stand out:
- Warehouse-to-Store assignment optimization: given the Fulfillment constraints this assignment
  implements (max warehouses per Store, max warehouses per Product per Store, max products per
  Warehouse), the choice of *which* Warehouse fulfills *which* Store isn't free - it's a real
  constrained-assignment problem, and the natural cost lever is minimizing transportation
  distance/cost subject to those constraints, rather than treating the constraints as pure
  correctness rules.
- Warehouse consolidation/replacement decisions (Scenario 5): each Location has a maximum capacity
  and a maximum number of Warehouses, so cost optimization here looks like "are we running multiple
  under-utilized Warehouses at a Location that could be consolidated into fewer, better-utilized
  ones" - a question this system can actually answer once utilization (stock vs. capacity) is
  tracked per Warehouse over time.
- Reducing fulfillment fragmentation: a Product being split across the maximum 2 Warehouses per
  Store, or a Store being split across the maximum 3 Warehouses, has a real transportation/handling
  cost; consolidating a Store's fulfillment onto fewer Warehouses (where capacity allows) trades off
  against resilience (single point of failure) and should be an explicit, measured decision rather
  than an accident of history.

Prioritization approach: start from the cost allocation data (Scenario 1) to find where the money
actually is - it's rarely intuitive which Warehouse/Store/Product combination is the biggest cost
driver until it's measured. Rank candidate initiatives by (estimated savings) / (implementation
effort and service-quality risk), and pilot the highest-ranked one on a subset of Warehouses/Stores
before rolling out network-wide, since fulfillment changes have direct customer-facing risk (a
Store no longer being served by its closest Warehouse affects delivery time). Expected outcomes
should be stated as testable hypotheses up front (e.g. "consolidating these two Warehouses reduces
overhead cost per unit by X% without increasing average fulfillment time beyond Y") so the pilot can
actually confirm or kill the initiative with data rather than opinion.

Questions I'd want answered: What service-quality metrics (delivery time, stock-out rate) are
currently tracked, so "without compromising service quality" is measurable rather than a
qualitative promise? Is there budget/appetite for physical changes (Warehouse replacement,
consolidation) versus only process/software-level changes? Who owns the tradeoff decision when a
cost optimization measurably increases risk to a specific Store's service level?
```

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
```txt
Without this integration, cost control becomes a manual reconciliation exercise between two
systems that each believe they hold the truth - which is slow, error-prone, and always looking
backward instead of forward. Integrating means Finance gets cost data that's tied directly to the
operational events that caused it (a Warehouse created/replaced/archived, a Store's stock changed,
a Product newly fulfilled from a Warehouse), rather than a monthly export someone reconciles by
hand. That's the main benefit: faster close cycles, and the ability to catch a cost anomaly (a
Warehouse suddenly running far over its allocated budget) while it's still actionable rather than
a quarter later.

This codebase already has a directly relevant precedent worth learning from: the `Store` entity
integrates with a legacy system (`LegacyStoreManagerGateway`), and the existing bug I fixed as part
of this assignment was that the legacy call fired *before* the database transaction had actually
committed - meaning the downstream system could receive data for a Store creation that then failed
to persist. I fixed it using CDI transactional events (fire the sync only after the transaction's
`AFTER_SUCCESS` phase), which is exactly the same principle a financial-systems integration would
need: never let an external system observe state that isn't durably true yet, or its ledger and this
system's will drift apart in ways that are extremely hard to detect and reconcile.

For a real-time financial integration I'd want an explicit design decision on:
- Push (fire-and-log an event per change, as I did for `Store`) vs. pull (Finance polls/subscribes
  to a change feed) - push is lower latency but needs its own retry/dead-letter handling since (as
  the `StoreLegacySyncListener` deliberately does today) a downstream failure after commit can't
  roll back the local change and must not be silently lost either.
- Idempotency: any event-based sync must be safe to replay (financial systems are especially
  intolerant of double-counted costs), which argues for each event carrying a stable
  identifier tied to the actual database row (the same reasoning behind keying Fulfillment
  associations on a Warehouse's database id rather than its reusable Business Unit Code, so history
  through a "replace" stays unambiguous).
- Reconciliation as a backstop, not a replacement for real-time sync - even a well-built real-time
  pipeline needs a periodic batch reconciliation to catch the failure modes real-time systems always
  eventually hit (a message lost, a service down during the event's delivery window).

Questions I'd want answered: What's the financial system's actual integration surface (event
stream/webhook, batch file, direct DB access) and its own consistency/idempotency guarantees? What
latency does "real-time" actually need to mean for this business (seconds? same business day?) -
that materially changes the architecture. Who is the source of truth when the two systems disagree,
and what's the resolution process?
```

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
```txt
Budgeting/forecasting matters here specifically because Warehouse capacity is a hard, discrete
constraint (`maxCapacity`, `maxNumberOfWarehouses` per Location in this system) - you can't
smoothly scale a Warehouse's capacity the way you might scale cloud compute, so a forecast that
says "Store demand at this Location will exceed available Warehouse capacity in 3 months" is
actionable well before it happens (open a new Warehouse, or replace an existing one with higher
capacity - Scenario 5), whereas the same finding discovered in hindsight is just a stock-out that
already happened.

What I'd take into account designing for this:
- Forecast at the same granularity the constraints actually bind at: capacity and warehouse-count
  limits are per-Location, so the forecasting unit of analysis should be "aggregate demand across
  all Stores served by Warehouses at this Location," not just per-Store demand in isolation -
  otherwise the forecast can look fine per-Store while the shared Location-level constraint is
  quietly being approached.
- Historical cost/volume data needs to survive organizational change events cleanly. The Warehouse
  "replace" operation this assignment implements is designed exactly for this: replacing a
  Warehouse archives the old one and creates a new one under the same Business Unit Code, so
  historical cost/volume time series for that Business Unit Code stay continuous across the
  transition rather than resetting - a forecasting model naively keyed on Warehouse identity alone
  would otherwise see a discontinuity that isn't real.
- Budget vs. actual variance needs to be attributable back to a specific driver (a new
  Store/Product fulfillment relationship added, a Warehouse capacity change) rather than just a
  number that moved - which again argues for the transactional/event history behind these changes
  being retained, not just current-state snapshots.
- Forecasts should carry a confidence/scenario range (best/expected/worst case) rather than a
  single number, precisely because fulfillment costs have lumpy, discrete step-changes (opening a
  Warehouse) rather than smooth ones.

Questions I'd want answered before designing this: What's the forecast horizon that's actually
decision-relevant (a Warehouse open/replace decision has a long lead time, so a 1-month forecast
may be nearly useless for that specific decision even if it's fine for short-term labor
scheduling)? What historical data actually exists today to train/validate a forecast against, and
for how far back? Is the ask a statistical forecast, or fundamentally a capacity-planning tool
where the "prediction" is closer to a what-if simulation the business runs interactively?
```

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
```txt
This is the scenario I could ground most directly in the actual implementation work, since it's
exactly what the `ReplaceWarehouseUseCase` I built enforces. Replacing a Warehouse isn't a
free-form update - it archives the currently active Warehouse and creates a brand-new database row
that reuses the same Business Unit Code, and I added two specific cost-control validations to that
operation beyond just "does the new Warehouse exist": the new Warehouse's capacity must be able to
accommodate the old Warehouse's carried-over stock (Capacity Accommodation), and the new
Warehouse's declared stock must exactly match the old Warehouse's stock (Stock Matching). Both
rules exist so that a replacement can't silently under-provision capacity relative to what's
actually being moved, and can't silently lose or fabricate inventory in the handoff - either of
which would corrupt the cost/inventory baseline the new Warehouse starts from.

Why preserving cost history matters: the Business Unit Code is the thing Finance tracks
budget/actuals against over time (it's the stable "account" for that area/business unit, per the
briefing). If replacing a Warehouse reset or discarded that history, every trend analysis, budget
variance report, and year-over-year comparison for that Business Unit Code would show a
discontinuity that has nothing to do with actual operational performance - making it impossible to
tell whether a cost change is due to the replacement itself or due to something that would need
attention regardless. My implementation keeps the old Warehouse row intact (archived, not deleted)
specifically so that historical cost data attached to it remains queryable, while the new row picks
up going forward - the Business Unit Code is the continuity key across that boundary.

This also directly connects to the bonus Fulfillment feature I built: I deliberately keyed
Warehouse-Product-Store fulfillment associations to the Warehouse's database id rather than its
Business Unit Code, precisely so that a replacement doesn't silently reattach historical
fulfillment/cost records to a different physical Warehouse just because it reused the same code -
new associations have to be created explicitly against the new Warehouse's id. That's a concrete
example of "preserving cost history" as a data-modeling decision, not just a policy statement.

Keeping the new Warehouse within budget going forward depends on that preserved history being
usable as the baseline: the old Warehouse's actual cost-per-unit, capacity utilization, and stock
levels are the most relevant reference point for setting the new Warehouse's budget expectations,
better than a generic Location-level average. Questions I'd want answered: is there a cost
attached to the physical replacement itself (decommissioning the old Warehouse, setting up the
new one) that needs its own budget line distinct from ongoing operational cost? Over what window
should the old Warehouse's archived cost history remain the reference baseline before the new
Warehouse is judged on its own independent performance?
```

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
