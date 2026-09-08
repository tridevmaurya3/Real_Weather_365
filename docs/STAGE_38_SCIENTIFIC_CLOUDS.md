# Stage 38 — Scientific Volumetric Clouds

## Scientific basis

The renderer follows the physical structure described by the WMO International Cloud Atlas:
cloud genera are separated by vertical level and by stratiform versus convective development.
The app does not claim to observe an individual cloud shape from forecast data. It infers a
plausible density profile from model fields and renders that profile continuously.

Primary references:

- WMO International Cloud Atlas: https://cloudatlas.wmo.int/en/home.html
- NOAA JetStream cloud formation: https://www.noaa.gov/jetstream/clouds
- Open-Meteo forecast variables: https://open-meteo.com/en/docs
- Android RuntimeShader / AGSL: https://developer.android.com/develop/ui/views/graphics/agsl
- Harris and Lastra, real-time cloud rendering: https://www.researchgate.net/publication/220789570_Real-Time_Cloud_Rendering

## Data-to-rendering pipeline

1. Open-Meteo supplies total, low, middle, and high cloud cover plus direct and diffuse radiation.
2. Current-hour precipitation and CAPE estimate optical thickness and convective development.
3. `CloudPhysicsEngine` selects one of ten WMO-inspired profiles and produces normalized base,
   top, erosion, optical depth, and instability parameters.
4. Android 13+ uses AGSL to ray-march a procedural 3D density field. Four-octave noise forms the
   large body and eroded edges. Wind direction advects the volume.
5. Beer-Lambert extinction, sun-direction samples, forward scattering, a powder term, and
   day/twilight/moon light create depth rather than painting opaque circles.
6. Android 8–12 uses the existing lightweight Canvas renderer so the supported minSdk remains 26.
7. Battery Saver / animation-off freezes advection while retaining the physically selected form.

## Honest limits

Forecast grids can provide cloud amount and atmospheric indicators, but not the exact cloud seen
above a specific phone. Exact nowcasting would require geostationary satellite imagery and/or a
camera-derived sky observation. Stage 38 therefore targets physically plausible, data-driven
clouds rather than claiming a pixel-identical observation.
