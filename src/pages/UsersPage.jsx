import { useState, useEffect, useCallback } from "react";
import {
  Box,
  Paper,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Button,
  TextField,
  MenuItem,
  Stack,
  Typography,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  Chip,
  IconButton,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import BlockIcon from "@mui/icons-material/Block";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import KeyIcon from "@mui/icons-material/VpnKey";
import Layout from "../components/Layout";
import * as userApi from "../api/userApi";
import * as rolemasterApi from "../api/rolemasterApi";
import * as hospitalApi from "../api/hospitalApi";

const emptyForm = {
  hospitalId: "",
  roleId: "",
  userName: "",
  fullName: "",
  email: "",
  password: "",
};

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [resetTarget, setResetTarget] = useState(null);
  const [newPassword, setNewPassword] = useState("");
  const [resetError, setResetError] = useState("");

  const loadAll = useCallback(async () => {
    setIsLoading(true);
    try {
      const [usersData, rolesData, hospitalsData] = await Promise.all([
        userApi.getUsers(),
        rolemasterApi.getRoles(),
        hospitalApi.getHospitals(),
      ]);
      setUsers(usersData);
      setRoles(rolesData);
      setHospitals(hospitalsData);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const roleNameById = (id) => {
    const match = roles.find((r) => String(r.id) === String(id));
    return match ? match.role_name : `#${id}`;
  };

  const hospitalNameById = (id) => {
    const match = hospitals.find((h) => String(h.id) === String(id));
    return match ? match.hospital_name : `#${id}`;
  };

  const openCreateDialog = () => {
    setForm({
      ...emptyForm,
      hospitalId: hospitals[0]?.id || "",
    });
    setError("");
    setCreateOpen(true);
  };

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await userApi.createUser(form);
      setCreateOpen(false);
      loadAll();
    } catch (err) {
      const errors = err.response?.data?.errors;
      setError(
        (errors && errors.join(", ")) ||
          err.response?.data?.message ||
          "Failed to create user.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleActive = async (user) => {
    if (user.is_active) {
      await userApi.deactivateUser(user.id);
    } else {
      await userApi.activateUser(user.id);
    }
    loadAll();
  };

  const openResetDialog = (user) => {
    setResetTarget(user);
    setNewPassword("");
    setResetError("");
  };

  const handleResetSubmit = async (e) => {
    e.preventDefault();
    setResetError("");
    try {
      await userApi.resetUserPassword(resetTarget.id, newPassword);
      setResetTarget(null);
    } catch (err) {
      const errors = err.response?.data?.errors;
      setResetError(
        (errors && errors.join(", ")) ||
          err.response?.data?.message ||
          "Failed to reset password.",
      );
    }
  };

  return (
    <Layout title="Users" subtitle="User Management">
      <Stack direction="row" justifyContent="flex-end" mb={3}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Create user
        </Button>
      </Stack>

      <Paper sx={{ border: "1px solid", borderColor: "divider" }}>
        {isLoading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
            <CircularProgress />
          </Box>
        ) : users.length === 0 ? (
          <Box sx={{ py: 6, textAlign: "center" }}>
            <Typography color="text.secondary">No users created yet.</Typography>
          </Box>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Username</TableCell>
                <TableCell>Full name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Hospital</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>{u.username}</TableCell>
                  <TableCell>{u.full_name}</TableCell>
                  <TableCell>{u.email || "\u2014"}</TableCell>
                  <TableCell>
                    <Chip size="small" label={roleNameById(u.role_id)} variant="outlined" />
                  </TableCell>
                  <TableCell>{hospitalNameById(u.hospital_id)}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={u.is_active ? "Active" : "Inactive"}
                      color={u.is_active ? "success" : "default"}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openResetDialog(u)} title="Reset password">
                      <KeyIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => toggleActive(u)} title={u.is_active ? "Deactivate" : "Activate"}>
                      {u.is_active ? (
                        <BlockIcon fontSize="small" color="error" />
                      ) : (
                        <CheckCircleIcon fontSize="small" color="success" />
                      )}
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      {/* Create user dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Create user</DialogTitle>
        <form onSubmit={handleCreateSubmit}>
          <DialogContent dividers>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <TextField
              select
              label="Hospital"
              fullWidth
              required
              margin="normal"
              value={form.hospitalId}
              onChange={handleChange("hospitalId")}
            >
              {hospitals.map((h) => (
                <MenuItem key={h.id} value={h.id}>
                  {h.hospital_name}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="Role"
              fullWidth
              required
              margin="normal"
              value={form.roleId}
              onChange={handleChange("roleId")}
            >
              {roles.map((r) => (
                <MenuItem key={r.id} value={r.id}>
                  {r.role_name} ({r.role_code})
                </MenuItem>
              ))}
            </TextField>

            <TextField
              label="Username"
              fullWidth
              required
              margin="normal"
              value={form.userName}
              onChange={handleChange("userName")}
            />

            <TextField
              label="Full name"
              fullWidth
              required
              margin="normal"
              value={form.fullName}
              onChange={handleChange("fullName")}
            />

            <TextField
              label="Email"
              type="email"
              fullWidth
              margin="normal"
              value={form.email}
              onChange={handleChange("email")}
            />

            <TextField
              label="Password"
              type="password"
              fullWidth
              required
              margin="normal"
              helperText="Minimum 8 characters"
              value={form.password}
              onChange={handleChange("password")}
            />
          </DialogContent>

          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? "Creating..." : "Create user"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Reset password dialog */}
      <Dialog open={Boolean(resetTarget)} onClose={() => setResetTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Reset password for {resetTarget?.username}</DialogTitle>
        <form onSubmit={handleResetSubmit}>
          <DialogContent dividers>
            {resetError && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {resetError}
              </Alert>
            )}
            <TextField
              label="New password"
              type="password"
              fullWidth
              required
              autoFocus
              margin="normal"
              helperText="Minimum 8 characters"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setResetTarget(null)}>Cancel</Button>
            <Button type="submit" variant="contained">
              Reset password
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Layout>
  );
}
