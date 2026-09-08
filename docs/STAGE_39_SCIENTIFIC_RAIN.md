# Stage 39 — Scientific Rain, Droplets and Wet Surfaces

## Research decisions

- A falling raindrop is not tear-shaped. Small drops are nearly spherical; larger drops become
  oblate with a flatter underside because aerodynamic pressure deforms them.
- Drizzle is below 0.5 mm. Drop terminal speed increases with diameter and approaches roughly
  10 m/s for the largest stable terrestrial raindrops.
- The visible rain field must contain a distribution of sizes and depths. Uniform parallel lines
  create a synthetic curtain and are avoided.
- Horizontal motion is the wind-vector component; vertical motion is based on terminal velocity.
- Splash radius and secondary droplets are driven by approximate impact energy. Surface wetness
  controls reflection strength separately from current drop count.

References:

- NASA GPM, Anatomy of a Raindrop: https://gpm.nasa.gov/education/videos/anatomy-raindrop
- NASA GPM FAQ, fall speed: https://gpm.nasa.gov/resources/faq
- NOAA/NWS glossary, drizzle size: https://forecast.weather.gov/glossary.php?word=dr
- Das et al., observed fall velocity: https://doi.org/10.1029/2019EA000956
- Rendering of Wet Materials: https://graphics.ucsd.edu/~henrik/papers/egwr99/rendering_wet_materials_egwr99.pdf

## Implementation

`RainMicrophysicsEngine` derives median diameter, still-air terminal speed, visible density,
wind drift, impact energy, wetness and mist from live precipitation, WMO code and wind.
Rain and storm scenes share the same depth-distributed field and impact renderer. Near drops are
rendered as flattened transparent ellipses, not icons. Lens droplets appear only at sufficient
intensity and remain deliberately sparse so they do not hide weather information. Animation-off
uses a stable frame and disables lightning flashes.

This is a visual microphysics approximation, not a claim that forecast data measures each drop.
