import { App as ConnectedApp } from './ConnectedApp';
import { ResultsDashboardDemo } from './demo/ResultsDashboardDemo';

export function App() {
  return window.location.pathname === '/demo/results' ? <ResultsDashboardDemo /> : <ConnectedApp />;
}
