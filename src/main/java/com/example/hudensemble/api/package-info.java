/**
 * Public API for the HudEnsemble plugin.
 *
 * <h2>Consumer usage (recommended)</h2>
 * <pre>{@code
 * // manifest.json:
 * // "Dependencies": { "com.example:HudEnsemble": "*" }
 *
 * import com.example.hudensemble.api.HudEnsemble;
 * import com.example.hudensemble.api.HudEnsembleClient;
 *
 * public class MyPlugin extends JavaPlugin {
 *   private HudEnsembleClient hudClient;
 *
 *   @Override
 *   protected void setup() {
 *     hudClient = HudEnsemble.openClient(this).orElse(null);
 *   }
 *
 *   @Override
 *   protected void shutdown() {
 *     if (hudClient != null) {
 *       hudClient.close();
 *       hudClient = null;
 *     }
 *   }
 * }
 * </pre>
 */
package com.example.hudensemble.api;
