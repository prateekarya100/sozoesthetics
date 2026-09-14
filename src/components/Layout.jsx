import { AppBar, Toolbar, Typography, Box, Chip, Button, Tabs, Tab } from "@mui/material";
import LocalHospitalIcon from "@mui/icons-material/LocalHospital";
import { useAuth } from "../context/AuthContext";
import { useNavigate, useLocation } from "react-router-dom";

const NAV_ITEMS = [
  { label: "Patients", path: "/patients" },
  { label: "Users", path: "/users" },
  { label: "Roles", path: "/roles" },
];

export default function Layout({ title, subtitle, children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const currentTab = NAV_ITEMS.some((item) => item.path === location.pathname)
    ? location.pathname
    : false;

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <AppBar
        position="static"
        color="transparent"
        sx={{ bgcolor: "background.paper", borderBottom: "1px solid", borderColor: "divider" }}
      >
        <Toolbar sx={{ py: 2, alignItems: "flex-start", flexDirection: "column", gap: 0.5 }}>
          <Box sx={{ display: "flex", width: "100%", alignItems: "center" }}>
            <Box sx={{ flexGrow: 1 }}>
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ letterSpacing: "0.08em" }}
              >
                {subtitle}
              </Typography>
              <Typography variant="h4" color="primary.main" fontWeight={800}>
                {title}
              </Typography>
            </Box>

            <Chip
              icon={<LocalHospitalIcon />}
              label={user ? `${user.fullName} \u00B7 ${user.roleCode}` : ""}
              variant="outlined"
              sx={{ mr: 2 }}
            />
            <Button variant="outlined" onClick={handleLogout}>
              Log out
            </Button>
          </Box>
        </Toolbar>

        <Tabs
          value={currentTab}
          onChange={(_, newPath) => navigate(newPath)}
          sx={{ px: 3 }}
        >
          {NAV_ITEMS.map((item) => (
            <Tab key={item.path} label={item.label} value={item.path} />
          ))}
        </Tabs>
      </AppBar>

      <Box sx={{ maxWidth: 1400, mx: "auto", px: 3, py: 4 }}>{children}</Box>
    </Box>
  );
}
