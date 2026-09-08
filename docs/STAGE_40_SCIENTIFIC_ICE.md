# Stage 40 — Scientific Snow, Ice and Hail Physics

## Scientific basis

Snow begins as hexagonal ice crystals. Temperature and water-vapour availability control the
crystal habit experienced during descent. Moist air around the dendritic growth zone (roughly
-12 to -18 C) favours branched dendrites, while other regimes favour plates, columns or needles.
Aggregation creates irregular flakes and riming creates rounder, faster graupel. Hail grows through
mixed-phase collisions inside thunderstorm updrafts; stronger updraft proxies support larger stones.

References:

- NOAA, snowflake formation: https://www.noaa.gov/stories/how-do-snowflakes-form-science-behind-snow
- NOAA/NESDIS snowflake simulator: https://www.nesdis.noaa.gov/about/k-12-education/ice-snow/snowflake-simulator
- NWS dendritic growth layer: https://www.weather.gov/media/wrh/online_publications/TAs/TA1604.pdf
- WRF microphysics overview: https://www2.mmm.ucar.edu/wrf/users/wrf_users_guide/build/html/physics.html
- NWS hail formation: https://www.weather.gov/wrn/spring2020-science-sm

## Rendering architecture

`IceMicrophysicsEngine` selects plate, column, needle, dendrite, aggregate or graupel using current
surface temperature, humidity and WMO weather code. Each habit has its own fall speed, flutter,
size, melt and sticking parameters. Near particles expose the six-fold or elongated crystal form;
far particles remain sub-pixel points. Wind and gusts affect drift independently.

Ground accumulation is reduced sharply above freezing instead of appearing permanently white.
Storm hail uses CAPE as an updraft proxy to control diameter and fall speed, and includes a layered
translucent ice appearance. Animation-off freezes particles and suppresses lightning.

The app has surface observations rather than a full vertical sounding, so crystal habit and hail
diameter are physically plausible inferences, not direct particle measurements.
