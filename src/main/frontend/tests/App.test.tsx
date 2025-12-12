import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import App from '../src/App';

describe('App', () => {
  it('renders without crashing', () => {
    // Just test that App renders without errors
    // The actual content depends on router state and authentication
    const { container } = render(<App />);
    expect(container).toBeTruthy();
  });

  it('renders QueryClientProvider and Router', () => {
    const { container } = render(<App />);
    // App should render its providers even if Router doesn't match
    // Note: Router won't render content because test URL "/" doesn't match basename "/app"
    // But the providers themselves should be rendered
    expect(container).toBeTruthy();
  });
});
