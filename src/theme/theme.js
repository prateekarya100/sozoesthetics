import { createTheme } from "@mui/material/styles";

// Centralized theme for the whole application. Every screen from now on
// should import `theme` from here rather than defining its own colors —
// this is what keeps the "Hospital User Hierarchy" dashboard look and the
// Patient Management screens visually consistent with each other, and with
// whatever gets built next.

const sozoTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#2563EB",
      dark: "#1D4ED8",
      light: "#60A5FA",
      contrastText: "#FFFFFF",
    },
    secondary: {
      main: "#0EA5E9",
      contrastText: "#FFFFFF",
    },
    success: {
      main: "#16A34A",
    },
    warning: {
      main: "#D97706",
    },
    error: {
      main: "#DC2626",
    },
    background: {
      default: "#F3F4F6",
      paper: "#FFFFFF",
    },
    text: {
      primary: "#111827",
      secondary: "#6B7280",
    },
    divider: "#E5E7EB",
  },
  shape: {
    borderRadius: 14,
  },
  typography: {
    fontFamily:
      '"Inter", "Roboto", "Helvetica Neue", "Arial", sans-serif',
    h1: { fontWeight: 800 },
    h2: { fontWeight: 800 },
    h3: { fontWeight: 700 },
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { fontWeight: 600, textTransform: "none" },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          paddingTop: 8,
          paddingBottom: 8,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
        },
      },
      defaultProps: {
        elevation: 0,
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          border: "1px solid #E5E7EB",
          borderRadius: 16,
          boxShadow: "none",
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        size: "small",
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 700,
          color: "#6B7280",
          textTransform: "uppercase",
          fontSize: "0.7rem",
          letterSpacing: "0.05em",
          backgroundColor: "#F9FAFB",
        },
      },
    },
  },
});

export default sozoTheme;
