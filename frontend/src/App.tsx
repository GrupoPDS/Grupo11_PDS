import { Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import DashboardLayout from './components/DashboardLayout';
import CatalogPage from './pages/CatalogPage';
import BookDetailsPage from './pages/BookDetailsPage';
import ReturnsPage from './pages/ReturnsPage';
import MyLoansPage from './pages/MyLoansPage';
import OverdueReportPage from './pages/OverdueReportPage';

function App() {
  return (
    <DashboardLayout>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route
          path="/books/:id"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'LIBRARIAN']}>
              <BookDetailsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/returns"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'LIBRARIAN']}>
              <ReturnsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/overdue-report"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'LIBRARIAN']}>
              <OverdueReportPage />
            </ProtectedRoute>
          }
        />
        <Route path="/my-loans" element={<MyLoansPage />} />
      </Routes>
    </DashboardLayout>
  );
}

export default App;
