import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import './i18n/config';
import { ErrorBoundary } from './shared/components';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found');
}

ReactDOM.createRoot(rootElement).render(
  <ErrorBoundary>
    <App />
  </ErrorBoundary>
);
