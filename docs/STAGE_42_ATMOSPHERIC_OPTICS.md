# Stage 42 — Sunlight, Sky Scattering and Atmospheric Optics

The clear-sky approximation uses solar elevation to estimate relative optical air mass. Rayleigh
scattering controls the blue molecular component; humidity and measured visibility provide a Mie
aerosol proxy. Low sun therefore travels through more atmosphere, loses more direct intensity and
reddens near the horizon. Live direct/diffuse radiation and cloud cover control exposure.

The implementation follows the principal relationships of physically based atmosphere models but
does not allocate Bruneton multi-scattering lookup textures on every Home frame. This preserves the
Android 8+ performance target while avoiding fixed time-of-day colour switches.

References:

- Bruneton, precomputed atmospheric scattering: https://ebruneton.github.io/
- Original real-time scattering paper: https://inria.hal.science/inria-00288758
- Clear-sky model evaluation: https://arxiv.org/abs/1612.04336
