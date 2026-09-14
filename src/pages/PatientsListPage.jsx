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
  InputAdornment,
  IconButton,
  Chip,
  Stack,
  Typography,
  CircularProgress,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import BlockIcon from "@mui/icons-material/Block";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import Layout from "../components/Layout";
import PatientFormDialog from "../components/PatientFormDialog";
import * as patientApi from "../api/patientApi";

export default function PatientsListPage() {
  const [patients, setPatients] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editingPatient, setEditingPatient] = useState(null);

  const loadPatients = useCallback(async () => {
    setIsLoading(true);
    try {
      const { data } = await patientApi.getPatients({ limit: 100, offset: 0 });
      setPatients(data);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPatients();
  }, [loadPatients]);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) {
      loadPatients();
      return;
    }
    setIsLoading(true);
    try {
      // A single search box tries name first — since name is a partial/ILIKE
      // match on the backend, it also naturally covers most real searches;
      // a purely numeric search term is more likely to be a mobile number.
      const isNumeric = /^[0-9]+$/.test(searchTerm.trim());
      const results = isNumeric
        ? await patientApi.searchPatients({ mobile: searchTerm.trim() })
        : await patientApi.searchPatients({ name: searchTerm.trim() });
      setPatients(results);
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateForm = () => {
    setEditingPatient(null);
    setFormOpen(true);
  };

  const openEditForm = (patient) => {
    setEditingPatient(patient);
    setFormOpen(true);
  };

  const handleSaved = () => {
    setFormOpen(false);
    loadPatients();
  };

  const toggleActive = async (patient) => {
    if (patient.is_active) {
      await patientApi.deactivatePatient(patient.id);
    } else {
      await patientApi.activatePatient(patient.id);
    }
    loadPatients();
  };

  return (
    <Layout title="Patients" subtitle="Patient Management">
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Box
          component="form"
          onSubmit={handleSearch}
          sx={{ display: "flex", gap: 1, width: 420 }}
        >
          <TextField
            fullWidth
            placeholder="Search by name or mobile number..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
          <Button type="submit" variant="outlined">
            Search
          </Button>
        </Box>

        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateForm}>
          Register patient
        </Button>
      </Stack>

      <Paper sx={{ border: "1px solid", borderColor: "divider", overflow: "hidden" }}>
        {isLoading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
            <CircularProgress />
          </Box>
        ) : patients.length === 0 ? (
          <Box sx={{ py: 6, textAlign: "center" }}>
            <Typography color="text.secondary">No patients found.</Typography>
          </Box>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>MRN</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Mobile</TableCell>
                <TableCell>Date of birth</TableCell>
                <TableCell>Gender</TableCell>
                <TableCell>Blood group</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {patients.map((p) => (
                <TableRow key={p.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700} color="primary.main">
                      {p.mrn}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {p.first_name} {p.last_name || ""}
                  </TableCell>
                  <TableCell>{p.mobile}</TableCell>
                  <TableCell>
                    {p.date_of_birth ? p.date_of_birth.substring(0, 10) : "\u2014"}
                  </TableCell>
                  <TableCell>{p.gender || "\u2014"}</TableCell>
                  <TableCell>{p.blood_group || "\u2014"}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={p.is_active ? "Active" : "Inactive"}
                      color={p.is_active ? "success" : "default"}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openEditForm(p)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => toggleActive(p)}>
                      {p.is_active ? (
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

      <PatientFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={handleSaved}
        editingPatient={editingPatient}
      />
    </Layout>
  );
}
