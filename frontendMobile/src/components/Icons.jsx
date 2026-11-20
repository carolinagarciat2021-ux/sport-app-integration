import React from 'react';

const base = { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.6, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true };

export const IconSearch = () => (
  <svg {...base}><circle cx="11" cy="11" r="7" /><line x1="16.5" y1="16.5" x2="21" y2="21" /></svg>
);

export const IconHeart = ({ lleno = false }) => (
  <svg {...base} fill={lleno ? 'currentColor' : 'none'}>
    <path d="M12 20.5s-7.5-4.6-9.2-9.3C1.6 7.9 3.6 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.4 0 5.4 3.4 4.2 6.7-1.7 4.7-9.2 9.3-9.2 9.3z" />
  </svg>
);

export const IconUser = () => (
  <svg {...base}><circle cx="12" cy="8" r="4" /><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" /></svg>
);

export const IconBag = () => (
  <svg {...base}><path d="M5 8h14l-1 12H6L5 8z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></svg>
);

export const IconMenu = () => (
  <svg {...base}><line x1="4" y1="7" x2="20" y2="7" /><line x1="4" y1="12" x2="20" y2="12" /><line x1="4" y1="17" x2="20" y2="17" /></svg>
);

export const IconClose = () => (
  <svg {...base}><line x1="6" y1="6" x2="18" y2="18" /><line x1="18" y1="6" x2="6" y2="18" /></svg>
);

export const IconWhatsApp = () => (
  <svg width="30" height="30" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true">
    <path d="M16.04 3C9.4 3 4 8.4 4 15.04c0 2.3.65 4.5 1.88 6.4L4 29l7.76-1.83a12 12 0 0 0 4.28.8C22.68 28 28 22.6 28 15.96 28 9.32 22.68 3 16.04 3zm0 21.9c-1.36 0-2.7-.3-3.9-.9l-.28-.14-4.6 1.08 1.1-4.46-.18-.3a9.9 9.9 0 0 1-1.5-5.1c0-5.5 4.5-10 10.06-10 5.5 0 9.96 4.5 9.96 10.06 0 5.5-4.5 9.76-9.66 9.76zm5.5-7.3c-.3-.15-1.8-.9-2.08-1-.28-.1-.48-.15-.68.15-.2.3-.78 1-.96 1.2-.18.2-.35.22-.65.08-.3-.15-1.28-.47-2.44-1.5-.9-.8-1.5-1.8-1.7-2.1-.18-.3-.02-.47.13-.62.14-.14.3-.35.45-.52.15-.18.2-.3.3-.5.1-.2.05-.38-.02-.53-.08-.15-.68-1.65-.93-2.25-.25-.6-.5-.5-.68-.5h-.58c-.2 0-.53.08-.8.38-.28.3-1.06 1.04-1.06 2.5 0 1.48 1.08 2.9 1.22 3.1.15.2 2.1 3.2 5.1 4.5.7.3 1.26.5 1.7.62.72.23 1.37.2 1.88.12.58-.08 1.8-.74 2.05-1.45.26-.7.26-1.3.18-1.45-.08-.13-.28-.2-.58-.35z" />
  </svg>
);
