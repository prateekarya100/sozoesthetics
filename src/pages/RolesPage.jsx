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
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import Layout from "../components/Layout";
import * as rolemasterApi from "../api/rolemasterApi";

const emptyForm = { roleName: "", roleCode: "", parentRoleId: "" };

export default function RolesPage() {
  const [roles, setRoles] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadRoles = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await rolemasterApi.getRoles();
      setRoles(data);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRoles();
  }, [loadRoles]);

  const roleNameById = (id) => {
    if (!id) return "\u2014 (top-level)";
    const match = roles.find((r) => String(r.id) === String(id));
    return match ? match.role_name : `#${id}`;
  };

  const openCreateDialog = () => {
    setForm(emptyForm);
    setError("");
    setDialogOpen(true);
  };

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await rolemasterApi.createRole({
        roleName: form.roleName,
        roleCode: form.roleCode.toUpperCase().replace(/\s+/g, "_"),
        parentRoleId: form.parentRoleId || null,
      });
      setDialogOpen(false);
      loadRoles();
    } catch (err) {
      const errors = err.response?.data?.errors;
      setError(
        (errors && errors.join(", ")) ||
          err.response?.data?.message ||
          "Failed to create role.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Layout title="Roles" subtitle="Role Management">
      <Stack direction="row" justifyContent="flex-end" mb={3}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Create role
        </Button>
      </Stack>

      <Paper sx={{ border: "1px solid", borderColor: "divider" }}>
        {isLoading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
            <CircularProgress />
          </Box>
        ) : roles.length === 0 ? (
          <Box sx={{ py: 6, textAlign: "center" }}>
            <Typography color="text.secondary">No roles created yet.</Typography>
          </Box>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Role name</TableCell>
                <TableCell>Role code</TableCell>
                <TableCell>Parent role</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {roles.map((role) => (
                <TableRow key={role.id} hover>
                  <TableCell>
                    <Typography fontWeight={600}>{role.role_name}</Typography>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={role.role_code} variant="outlined" />
                  </TableCell>
                  <TableCell>{roleNameById(role.parent_role_id)}</TableCell>
                  <TableCell>
                    {new Date(role.created_at).toLocaleDateString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Create role</DialogTitle>
        <form onSubmit={handleSubmit}>
          <DialogContent dividers>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <TextField
              label="Role name"
              fullWidth
              required
              margin="normal"
              placeholder="e.g. Senior Doctor"
              value={form.roleName}
              onChange={handleChange("roleName")}
            />

            <TextField
              label="Role code"
              fullWidth
              required
              margin="normal"
              placeholder="e.g. SENIOR_DOCTOR"
              helperText="Uppercase, no spaces — used internally for permission checks"
              value={form.roleCode}
              onChange={handleChange("roleCode")}
            />

            <TextField
              select
              label="Parent role"
              fullWidth
              margin="normal"
              helperText="Leave blank for a top-level role (e.g. Admin)"
              value={form.parentRoleId}
              onChange={handleChange("parentRoleId")}
            >
              <MenuItem value="">
                <em>None (top-level role)</em>
              </MenuItem>
              {roles.map((role) => (
                <MenuItem key={role.id} value={role.id}>
                  {role.role_name}
                </MenuItem>
              ))}
            </TextField>
          </DialogContent>

          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? "Creating..." : "Create role"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Layout>
  );
}
