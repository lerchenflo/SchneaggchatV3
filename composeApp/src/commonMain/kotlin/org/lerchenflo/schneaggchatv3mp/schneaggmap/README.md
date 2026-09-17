# Schneaggmap attributes

How map entry attributes are modelled, what changed in the attribute rework, and how to extend it.
This file exists in both repos (server: `schneaggmap/README.md`, client: `schneaggmap/README.md`) - keep them in sync.

## Model

Every map entry has a list of `LocationData` (one per location type, e.g. `Camping`). Each attribute of a
type is described by two parts:

| Part | What it is | Sent over the wire / stored in Mongo? |
|---|---|---|
| `AttributeValue` | The actual value: `StringValue`, `IntValue`, `DoubleValue`, `LongValue`, `BoolValue` | **Yes** - `{ "_class": "string", "value": ... }` in Mongo, the JSON API and the client's Room cache |
| `AttributeDefinition` | The *meaning* of an attribute: its type, whether it is required, limits, enum options | **No** - lives only in code, returned by each type's `schema()` |

Because definitions are code-only, new definition types can be added freely. A new `AttributeValue`
subtype, a renamed property, or a changed stored value type **is** a wire/DB change: older app versions
can no longer decode it, so the app has to ship first and existing data needs a migration.

Property name, `AttributeKey` constant, JSON key and Mongo field name are always the same identifier
(`CAMPING_KIND` <-> `campingKind`).

## Definitions

| Definition | Stored as | Client UI | Server validation |
|---|---|---|---|
| `StringDef(maxLength)` | `StringValue` | Text field | Length |
| `IntDef(min, max)` | `IntValue` | Number field | Range |
| `DoubleDef(min, max)` | `DoubleValue` | Decimal field | Range |
| `LongDef(min, max)` | `LongValue` | Number field (plain long - **no longer a date picker**) | Range |
| `BoolDef` | `BoolValue` | Switch | Type |
| `EnumDef(options)` | `StringValue` holding the enum **constant name** (never the ordinal) | Dropdown with translated labels; unknown names are shown raw | Value must be one of `options` |
| `DateTimeDef` | `LongValue` (epoch millis) | Date + time picker | Type |
| `PriceDef(max)` | `DoubleValue` (euros) | Decimal field, `€` suffix, max 2 decimals, `.` or `,` | Not negative, max |
| `DistanceDef(unit, max)` | `DoubleValue` in `unit` (`METERS`, `KILOMETERS`) | Decimal field with unit suffix | Not negative, max |
| `RatingDef(min, max)` | `IntValue` | Slider, value saved on release | Within `min..max` |
| `SecretDef(maxLength)` | `StringValue` | Hidden text with show + copy buttons (copy does not echo the value in a snackbar) | Length |

Validation (`SchneaggmapService.validate`) checks **every present value**, required or not. A missing value
only fails when the definition is `required`.

On the client, `EnumDef.options` is `List<AttributeOption>` (each option carries its label resource); on the
server it is `List<String>`.

## Enums

Defined in `model/AttributeOptions.kt` (server) and `domain/AttributeOptions.kt` (client). Constant names are
the stored values - **never rename a constant without a migration, only add new ones**.

| Enum | Options | Used by |
|---|---|---|
| `BicycleUndergroundType` | `ASPHALT`, `GRAVEL`, `DIRT`, `TRAIL`, `OTHER` | `BICYCLE_UNDERGROUND_TYPE` |
| `VenueSetting` | `INDOOR`, `OUTDOOR`, `BOTH` | `SWIMMING_SETTING`, `CLIMBINGSPOT_SETTING`, `VOLLEYBALL_SETTING` |
| `OffroadDiscipline` | `MOTOCROSS`, `ENDURO`, `BOTH` | `OFFROAD_MOTORCYCLE_DISCIPLINE` |
| `CampingKind` | `OFFICIAL_SITE`, `WILD_CAMPING`, `TOLERATED` | `CAMPING_KIND` |

Client labels: `location_bicycle_underground_type_<option>` and `location_option_<enum>_<option>` in
`values`, `values-de` and `values-it`.

## What changed, per location type

Stored-data changes are marked **(migrated)** - existing Mongo data is converted on server start.

| Type | Attribute | Before | After |
|---|---|---|---|
| Camping | `campingOfficial` -> `campingKind` **(migrated)** | `BoolDef`, required | `EnumDef(CampingKind)`, required |
| Camping | `campingWaterDistance` **(migrated)** | `IntDef` | `DistanceDef(METERS)` - stored double |
| Swimming | `swimmingIndoor` -> `swimmingSetting` **(migrated)** | `BoolDef` | `EnumDef(VenueSetting)` |
| Swimming | `swimmingPrice` **(migrated)** | `IntDef` | `PriceDef` - stored double |
| Climbing spot | `climbingspotOutdoor` -> `climbingspotSetting` **(migrated)** | `BoolDef` | `EnumDef(VenueSetting)` |
| Climbing spot | `climbingspotPrice` **(migrated)** | `IntDef` | `PriceDef` - stored double |
| Volleyball | `volleyballOutdoor` -> `volleyballSetting` **(migrated)** | `BoolDef` | `EnumDef(VenueSetting)` |
| Offroad motorcycle | `offroadMotorcycleMotocross` + `offroadMotorcycleEnduro` -> `offroadMotorcycleDiscipline` **(migrated)** | two `BoolDef` | `EnumDef(OffroadDiscipline)` |
| Bicycle | `bicycleUndergroundType` **(migrated)** | `StringDef` (free text) | `EnumDef(BicycleUndergroundType)` |
| Bicycle | `bicycleDifficulty` | `IntDef(1..10)` | `RatingDef(1..10)` |
| Mountain street | `mountainStreetMautFee` | `DoubleDef` | `PriceDef` |
| Mountain street | `mountainStreetHeightLimit` | `DoubleDef` | `DistanceDef(METERS)` |
| Wifi | `wifiPassword` | `StringDef` | `SecretDef` |
| Police | `policeLastSeen` | `LongDef` | `DateTimeDef` |
| Horse riding | `horseRidingNextTournament` | `LongDef` | `DateTimeDef` |
| Sightseeing / Party | `sightseeingEntryFee`, `partyEntryFee` | `DoubleDef` | `PriceDef` |
| Food kebab / pizza / burger / beer / ice | `food*Price` | `DoubleDef` | `PriceDef` |

The renamed properties are nullable (client: `= null` default) so documents and cached entries without the
new field still decode.

### New location type

| Type | Discriminator | Attributes |
|---|---|---|
| `FoodAustrian` / `FOOD_AUSTRIAN` | `food_austrian` | none (like `FoodGreek`) |

## Migration (`MainController.migrateMapAttributeTypes`)

Runs on startup right after `migrateMapAttributeKeys`. Idempotent - an element is only touched while it still
has an old field or old value type.

| Old data | New data |
|---|---|
| `campingOfficial` true / false / missing | `campingKind` `OFFICIAL_SITE` / `WILD_CAMPING` / `OFFICIAL_SITE` |
| `swimmingIndoor` true / false | `swimmingSetting` `INDOOR` / `OUTDOOR` |
| `climbingspotOutdoor`, `volleyballOutdoor` true / false | `*Setting` `OUTDOOR` / `INDOOR` |
| motocross + enduro both / one / none | `BOTH` / `MOTOCROSS` or `ENDURO` / field left empty |
| Bicycle underground free text (case-insensitive, first match wins) | contains `asphalt`/`teer` -> `ASPHALT`, `schotter`/`kies`/`gravel` -> `GRAVEL`, `dirt`/`erde` -> `DIRT`, `trail` -> `TRAIL`, anything else -> `OTHER`, blank -> removed |
| Swimming / climbing price, camping water distance as int | same number as double |

It converts both `map_entries` and the `locationData` JSON snapshots in `map_entry_versions`, so undoing an
old change can't bring the old shape back. Every migrated entry gets `updatedAt` bumped by 1 ms so clients
re-sync it (map sync only sends entries newer than the client's copy).

**Back up `map_entries` and `map_entry_versions` before the first production run** - old values are overwritten.

## Compatibility / rollout

- Release the app update **before** deploying this server. Older app versions expect `campingOfficial`,
  `swimmingIndoor`, `climbingspotOutdoor` and `volleyballOutdoor` (non-defaulted) and fail to decode those
  entries; they also can't decode `food_austrian` entries.
- Older apps still send the old shapes when saving; the server rejects Camping without `campingKind` (400).
- Adding an enum option is safe: stored values are names, and unknown names are shown raw on older clients.

## Icons (client only)

`LocationType.drawableRes()` in `domain/LocationData.kt`. All icons are 1024x1024 transparent PNGs in a flat
style with bold black outlines.

- New: `icon_food_austrian` (schnitzel + flag), `icon_climbingspot` (climbing wall, was the swimming icon),
  `icon_food_cafe_bakery` (coffee + croissant, was the generic food icon).
- Redrawn to fit their purpose / the common style: `icon_doener` (döner spit, looked like a taco),
  `icon_outdoor_fitness` (outdoor bars, was a gym dumbbell), `icon_sightseeing` and `icon_partylocation`
  (no map pin inside the marker), `icon_food` (plate + cutlery), `icon_wifi` (coloured), `icon_camping`
  (bold outlines).
- All other icons are unchanged.

`icon_schneagg_game.png` (Supercell-style game icon of the Schneagg with a chat bubble in its shell) is a
standalone image and not referenced from code yet.

## How to extend

**New enum attribute**
1. Server: add the enum to `model/AttributeOptions.kt`, use `EnumDef(key, required, options = X.entries.map { it.name })`.
2. Client: add the same enum to `domain/AttributeOptions.kt` implementing `AttributeOption` with a label per
   option, use `EnumDef(key, required, options = X.entries)`.
3. Add option labels to `values`, `values-de`, `values-it`.

**New definition type** (reusing an existing `AttributeValue`)
1. Add the subtype to `AttributeDefinition` on both sides (server: `@JsonSubTypes`, client: `@SerialName`).
2. Server: add a branch in `SchneaggmapService.validate` (the compiler forces it).
3. Client: add a branch in `KeyValueView` (the compiler forces it).

**New location type** - follow the checklist at the top of `LocationData.kt` in each repo.

**Changing a stored shape** (rename, value type change) - add an idempotent step to `migrateMapAttributeTypes`
(or a new migration after it), convert `map_entry_versions` snapshots too, bump `updatedAt`, and ship the app first.
