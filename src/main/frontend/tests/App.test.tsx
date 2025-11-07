import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '../src/App';

describe('App', () => {
  it('renders the app title', () => {
    render(<App />);
    expect(screen.getByText(/Dev Debug Platform - React Migration/i)).toBeInTheDocument();
  });

  it('shows initialization message', () => {
    render(<App />);
    expect(screen.getByText(/Frontend React application is initializing/i)).toBeInTheDocument();
  });
});
