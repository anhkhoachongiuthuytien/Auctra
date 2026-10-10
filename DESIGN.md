# Auctra interface

The JavaFX client is a task interface for buyers, sellers and administrators. Its existing Auctra name, logo and blue accent are retained. Hierarchy and layout follow the Operate, layout, clarity and accessibility guidance downloaded from [Impeccable](https://impeccable.style/).

## Visual system

- Segoe UI: body 14px, sections 18px, page titles 28px, prices 36px.
- Blue `#2457d6` marks primary actions. Neutral surfaces carry navigation and content. Status uses text as well as color.
- Light: background `#f5f7fa`, panel `#ffffff`, text `#182438`. Dark: background `#101722`, panel `#182232`, text `#edf2fa`.
- Buttons and inputs have 7px corners, cards 12px. Use a border or surface change; avoid decorative gradients and stacked shadows.
- Live styles enter through `css/app.css`: tokens, themes and `interface.css`.

## Layout and behavior

- Minimum window: 800×600. Initial window: 1180×760.
- Compact navigation uses 176px and retains readable labels.
- Auth forms scroll vertically and hide the brand panel below 1000px.
- `ResponsiveColumns` measures height from available width and stacks below each surface's breakpoint. Compact auction detail puts bidding first.
- Price, remaining time and manual bidding precede autobid. History and chart follow. The bid action fits the first viewport at both supported sizes with the fixture title.
- One search per list. Cards wrap. Seller creation expands inline; operations remain visible as buttons.
- Forms use visible labels. Auction cards and buttons support keyboard activation. Empty messages do not reserve rows; errors remain visible and toasts wrap.
- The admin chart counts stored auction states. No invented activity or verification claims.
- Newest bids appear first. Quick increments preserve cents.

## Validation

`UiInteractionTest` checks eight screens at both sizes and themes, real manual/autobid buttons, seller validation, multiline descriptions and chart counts. Existing authentication, concurrency and socket tests remain in the build.

Regenerate 32 native previews with an isolated temporary database:

```powershell
mvn -pl auction-client javafx:run '-Djavafx.args=--screenshots'
```

Output: `auction-client/target/ui-screenshots`. Preview descriptions are fixtures, not marketplace listings. Captures do not contact a production server.
