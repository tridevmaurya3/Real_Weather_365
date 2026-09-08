# Stage 41 — Fog, Mist, Haze and Visibility Physics

Measured visual range is converted to an atmospheric extinction coefficient using the classical
Koschmieder relation. Scene contrast therefore falls exponentially with distance instead of using
one uniform grey overlay. Temperature minus dew point supplies a condensation-likelihood signal;
relative humidity and WMO fog codes support it, while PM2.5/AQI remain a separate dry-haze signal.

Dense fog follows the NOAA/NWS threshold near 0.4 km visibility. Fog bands advect with the wind,
near and distant layers receive different transmittance, and droplet fog creates restrained
forward-scattered light halos. Animation-off retains a deterministic static atmosphere.

References:

- NOAA humidity/dew point: https://www.nesdis.noaa.gov/about/k-12-education/atmosphere/what-humidity
- NOAA fog glossary: https://www.noaa.gov/jetstream/appendix/weather-glossary-f
- Lee and Shang, visibility/Koschmieder model: https://doi.org/10.1175/JAS-D-16-0102.1

The renderer uses surface observations, so it cannot distinguish every fog mechanism without a
vertical temperature profile. It represents observed visual extinction without claiming that.
