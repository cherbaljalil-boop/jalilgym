# Body heatmap artwork

`body.py` is the single source of truth for the front/back body regions used by
`BodyHeatmap` (`core/ui/.../body`). Shapes are drawn for the left half and mirrored.

```bash
# Regenerate the Kotlin artwork after editing body.py
python3 tools/heatmap/body_kotlin.py tools/heatmap core/ui/src/main/kotlin/com/chdjalil/jalilgym/core/ui/body/BodyArtwork.kt

# Optional: write an SVG preview (open it in a browser)
python3 tools/heatmap/body_preview.py tools/heatmap /tmp/body.svg
```
