import { useState } from "react";

export function ControlBar({
  onRun,
  running,
  gatewayMode,
}: {
  onRun: (size: number, seed: number, apiKey: string) => void;
  running: boolean;
  gatewayMode: string | null;
}) {
  const [size, setSize] = useState(60);
  const [seed, setSeed] = useState(42);
  const [apiKey, setApiKey] = useState("dev-local-key");

  return (
    <div className="control-bar">
      <div className="control-bar__field">
        <label htmlFor="size">Batch size</label>
        <input id="size" type="number" min={1} max={2000} value={size} onChange={(e) => setSize(Number(e.target.value))} />
      </div>
      <div className="control-bar__field">
        <label htmlFor="seed">Seed</label>
        <input id="seed" type="number" value={seed} onChange={(e) => setSeed(Number(e.target.value))} />
      </div>
      <div className="control-bar__field control-bar__field--grow">
        <label htmlFor="apiKey">X-API-Key</label>
        <input id="apiKey" type="text" value={apiKey} onChange={(e) => setApiKey(e.target.value)} />
      </div>
      <button className="control-bar__run" disabled={running} onClick={() => onRun(size, seed, apiKey)}>
        {running ? "Running..." : "Run recovery batch"}
      </button>
      {gatewayMode && <span className={`gateway-badge gateway-badge--${gatewayMode}`}>gateway: {gatewayMode}</span>}
    </div>
  );
}
