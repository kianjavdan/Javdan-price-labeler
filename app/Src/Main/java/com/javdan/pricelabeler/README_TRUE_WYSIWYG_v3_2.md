Javdan Price Labeler v3.2 — Equal Card Height + Manual Resize

Changes:
1. Auto Height now calculates the natural height required by every visible price card, takes the tallest one, and applies that SAME height to all cards.
2. Disabling Auto Height enables practical manual resizing through the new "ارتفاع دستی کادرها" slider (35–300 logical px), with live preview.
3. Manual card height is persisted in SharedPreferences/templates and is applied to Preview, single export, and Batch Export through the shared renderer.
4. The outer label frame height is derived from the real sum of card heights + gaps + panel padding in both modes, avoiding unused vertical space.
5. Existing TRUE-WYSIWYG fixed canvas and aspect-ratio-preserving product rendering remain unchanged.
