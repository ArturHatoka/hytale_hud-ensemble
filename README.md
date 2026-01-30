# HUD Ensemble

HUD Ensemble is a server-side Hytale plugin that enables **multi-layer `CustomUIHud` composition** per player, so multiple mods/systems can show HUD elements **at the same time** instead of constantly overwriting each other via `HudManager#setCustomHud(...)`.

It provides a public **API for other plugins**, including:
- layer management (add / replace / remove),
- namespacing (to avoid ID collisions between plugins),
- fast in-place updates inside a layer (`updateLayer`),
- broadcast helpers (“show/remove for all online players”).

---

## How it works

In Hytale, a player can have only **one** `CustomUIHud` set at a time. HUD Ensemble installs a composite HUD (`MultipleCustomUIHud`) that:

- creates a root group `#HudEnsemble`,
- creates a child group `#<layer>` for each layer under `#HudEnsemble`,
- builds each layer and **isolates** its UI commands inside the corresponding child group,
- supports partial command updates via `updateLayer(...)`.

Note: the implementation uses a reflection bridge to access HUD build commands. If a future Hytale update changes internals, the plugin may fall back to “single HUD” mode.

---

## Installation

1. Build the plugin `.jar`.
2. Put it into your server’s plugins folder.
3. Start the server.

---

## Demo commands

These commands exist only for testing on a server. For real integrations, use the API (see below).

- `/hudens demo` — enables demo HUD layers.
- `/hudens clean` — removes demo HUD layers.

---

## Quick start for other plugins

### Get the service / open a client

    import com.example.hudensemble.api.HudEnsemble;
    import com.example.hudensemble.api.HudEnsembleClient;

    public class MyPlugin extends JavaPlugin {
      private HudEnsembleClient hud;

      @Override
      protected void setup() {
        hud = HudEnsemble.openClientOrThrow(this);
      }

      @Override
      protected void shutdown() {
        if (hud != null) {
          hud.close(); // important!
          hud = null;
        }
      }
    }

`HudEnsembleClient` automatically namespaces your `layerId`s so different plugins do not collide.

---

## Show a HUD to a specific player

    hud.setLayer(player, playerRef, "greeting", new MyHud());

Remove a layer from a player:

    hud.removeLayer(player, "greeting");

Remove all layers owned by this client from a player:

    hud.clear(player);

---

## Update data inside a layer (no rebuild)

If a layer already exists, you can update UI commands in-place:

    hud.updateLayer(player, "greeting", cmd -> {
      cmd.set("#Title.Text", "Hello, HUD Ensemble!");
    });

Selectors inside `updateLayer` are written **normally**; the plugin automatically prefixes them to the correct layer root.

---

## Broadcast: show/remove for all online players

Show a layer for all connected players:

    int scheduled = hud.setLayerForAllOnline("watermark", pr -> new MyHud());

Remove a specific layer for all online players:

    int scheduled = hud.removeLayerForAllOnline("watermark");

Clear all layers owned by this client for all online players:

    int scheduled = hud.clearForAllOnline();

The returned value is the number of players for which the action was **scheduled** (execution happens via `world.execute(...)`).

---

## Compatibility notes

### 1) Other mods' HUDs without using this API
- If another plugin sets a `CustomUIHud` **before** HUD Ensemble is activated for a player, that HUD is preserved as a “base layer” and will continue to display.
- If another plugin sets a `CustomUIHud` **after** the ensemble is active, it will overwrite the composite HUD (that’s how `setCustomHud` works), and HUD Ensemble layers will disappear.

### 2) Fallback mode (multi-layer not available)
If the reflection bridge is not available, HUD Ensemble may fall back to “single HUD override” (setting the HUD directly). In that mode, multi-layer behavior and remove/update per layer will not work as expected.

### 3) Cleanup responsibilities
- Always call `HudEnsembleClient#close()` in your plugin’s `shutdown()`.
- A “safety net” exists: if a client becomes unreachable (GC), a `Cleaner` performs best-effort cleanup. This does not replace proper `close()` usage.

---

## API Versioning

The API exposes a version number:

    int v = HudEnsemble.getServiceOrThrow().getApiVersion();

Incompatible changes will bump this version.

---

## License
MIT
