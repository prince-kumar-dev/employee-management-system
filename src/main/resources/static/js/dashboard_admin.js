document.addEventListener('DOMContentLoaded', function() {
    const loggedInUser = getLoggedInUser(); // From common.js
    // Crucial: Ensure loggedInUser and loggedInUser.id are available
    if (!loggedInUser || loggedInUser.role !== 'ADMIN' || !loggedInUser.id) {
        alert('Access Denied or Admin ID missing. You must be an Admin to view this page.');
        window.location.href = 'login.html';
        return; // Stop further execution
    }
    // Pass the admin ID to loadDashboardData
    loadDashboardData(loggedInUser.id);
});

let employeeRoleChartInstance = null;
let avgSalaryChartInstance = null;
let employeeDeptCountChartInstance = null;
let employeeGenderChartInstance = null;
let employeeAgeGroupChartInstance = null;

// Modified to accept adminId
async function loadDashboardData(adminId) {
    const apiEndpoint = '/api/dashboard/summary';

    const totalEmployeesStatEl = document.getElementById('totalEmployeesStat');
    const totalDepartmentsStatEl = document.getElementById('totalDepartmentsStat');
    const averageAgeStatEl = document.getElementById('averageAgeStat');

    const roleChartCanvas = document.getElementById('employeeRoleChart')?.getContext('2d');
    const salaryChartCanvas = document.getElementById('avgSalaryChart')?.getContext('2d');
    const deptCountChartCanvas = document.getElementById('employeeDeptCountChart')?.getContext('2d');
    const genderChartCanvas = document.getElementById('employeeGenderChart')?.getContext('2d');
    const ageGroupChartCanvas = document.getElementById('employeeAgeGroupChart')?.getContext('2d');

    if (totalEmployeesStatEl) totalEmployeesStatEl.textContent = 'Loading...';
    if (totalDepartmentsStatEl) totalDepartmentsStatEl.textContent = 'Loading...';
    if (averageAgeStatEl) averageAgeStatEl.textContent = 'Loading...';

    // Prepare headers
    const headers = { 'X-Admin-Id': String(adminId) };

    try {
        // **** FIX: Pass the 'headers' object to makeApiCall ****
        const summaryData = await makeApiCall(apiEndpoint, 'GET', null, headers, true, "Loading dashboard...");

        console.log("Dashboard Summary Data:", summaryData); // DEBUG: Check received data

        if (!summaryData) {
            // This might happen if makeApiCall returns null for a 204 or non-JSON success,
            // but /api/dashboard/summary should always return JSON.
            // So, if summaryData is null here, it's more likely an issue with makeApiCall or the API itself.
            throw new Error('No data received from dashboard summary API or API returned null.');
        }

        // 1. Populate Statistic Widgets
        if (totalEmployeesStatEl) totalEmployeesStatEl.textContent = summaryData.totalEmployees !== undefined ? String(summaryData.totalEmployees) : '0';
        if (totalDepartmentsStatEl) totalDepartmentsStatEl.textContent = summaryData.totalDepartments !== undefined ? String(summaryData.totalDepartments) : '0';
        if (averageAgeStatEl) {
            averageAgeStatEl.textContent = summaryData.averageEmployeeAge ? summaryData.averageEmployeeAge.toFixed(1) + ' years' : '- years';
        }

        // 2. Render Employee Count by Role Chart
        if (roleChartCanvas && summaryData.employeeCountByRole) {
            if (employeeRoleChartInstance) employeeRoleChartInstance.destroy();
            employeeRoleChartInstance = new Chart(roleChartCanvas, {
                type: 'doughnut',
                data: {
                    labels: Object.keys(summaryData.employeeCountByRole),
                    datasets: [{
                        label: 'Employee Count by Role',
                        data: Object.values(summaryData.employeeCountByRole),
                        backgroundColor: ['rgba(54, 162, 235, 0.7)', 'rgba(255, 206, 86, 0.7)', 'rgba(75, 192, 192, 0.7)'],
                        borderColor: ['rgba(54, 162, 235, 1)', 'rgba(255, 206, 86, 1)', 'rgba(75, 192, 192, 1)'],
                        borderWidth: 1
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
            });
            hideChartLoadingPlaceholder(roleChartCanvas);
        } else if (roleChartCanvas) {
            showChartMessage(roleChartCanvas, 'No role data available.');
        }

        // 3. Render Average Salary per Department Chart
        if (salaryChartCanvas && summaryData.averageSalaryPerDepartment && summaryData.averageSalaryPerDepartment.length > 0) {
            if (avgSalaryChartInstance) avgSalaryChartInstance.destroy();
            avgSalaryChartInstance = new Chart(salaryChartCanvas, {
                type: 'bar',
                data: {
                    labels: summaryData.averageSalaryPerDepartment.map(item => item.departmentName),
                    datasets: [{
                        label: 'Average Salary',
                        data: summaryData.averageSalaryPerDepartment.map(item => item.averageSalary),
                        backgroundColor: 'rgba(75, 192, 192, 0.7)',
                        borderColor: 'rgba(75, 192, 192, 1)',
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { callback: value => '₹' + value.toLocaleString() } } },
                    plugins: { legend: { display: false } }
                }
            });
            hideChartLoadingPlaceholder(salaryChartCanvas);
        } else if (salaryChartCanvas) {
            showChartMessage(salaryChartCanvas, 'No salary data available.');
        }

        // 4. Employee Count by Department
        if (deptCountChartCanvas && summaryData.employeeCountByDepartment) { // Ensure DTO has this field
            const deptNames = Object.keys(summaryData.employeeCountByDepartment);
            const deptCounts = Object.values(summaryData.employeeCountByDepartment);
            if (employeeDeptCountChartInstance) employeeDeptCountChartInstance.destroy();
            employeeDeptCountChartInstance = new Chart(deptCountChartCanvas, {
                type: 'bar',
                data: { labels: deptNames, datasets: [{ label: 'Employees', data: deptCounts, backgroundColor: 'rgba(255, 159, 64, 0.7)', borderColor: 'rgba(255, 159, 64, 1)', borderWidth: 1 }] },
                options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0 } } }, plugins: { legend: { display: false } } }
            });
            hideChartLoadingPlaceholder(deptCountChartCanvas);
        } else if (deptCountChartCanvas) {
             showChartMessage(deptCountChartCanvas, 'No department count data.');
        }

        // 5. Employee Gender Distribution
        if (genderChartCanvas && summaryData.employeeCountByGender) { // Ensure DTO has this field
            const genderLabels = Object.keys(summaryData.employeeCountByGender);
            const genderCounts = Object.values(summaryData.employeeCountByGender);
            const genderColors = { 'MALE': 'rgba(54, 162, 235, 0.7)', 'FEMALE': 'rgba(255, 99, 132, 0.7)', 'OTHER': 'rgba(255, 206, 86, 0.7)', 'PREFER_NOT_TO_SAY': 'rgba(153, 102, 255, 0.7)', 'UNSPECIFIED': 'rgba(201, 203, 207, 0.7)'};
            const backgroundColors = genderLabels.map(label => genderColors[label.toUpperCase()] || genderColors['UNSPECIFIED']); // Ensure robust key matching
            const borderColors = backgroundColors.map(color => color.replace('0.7', '1'));

            if (employeeGenderChartInstance) employeeGenderChartInstance.destroy();
            employeeGenderChartInstance = new Chart(genderChartCanvas, {
                type: 'pie',
                data: { labels: genderLabels, datasets: [{ label: 'Gender', data: genderCounts, backgroundColor: backgroundColors, borderColor: borderColors, borderWidth: 1 }] },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
            });
            hideChartLoadingPlaceholder(genderChartCanvas);
        } else if (genderChartCanvas) {
             showChartMessage(genderChartCanvas, 'No gender data available.');
        }

        // 6. Employee Age Group Distribution
        if (ageGroupChartCanvas && summaryData.employeeCountByAgeGroup) { // Ensure DTO has this field
            const orderedAgeGroupLabels = ["Under 20", "20-29", "30-39", "40-49", "50-59", "60+"];
            const orderedAgeGroupCounts = orderedAgeGroupLabels.map(label => summaryData.employeeCountByAgeGroup[label] || 0);

            if (employeeAgeGroupChartInstance) employeeAgeGroupChartInstance.destroy();
            employeeAgeGroupChartInstance = new Chart(ageGroupChartCanvas, {
                type: 'bar',
                data: { labels: orderedAgeGroupLabels, datasets: [{ label: 'Employees', data: orderedAgeGroupCounts, backgroundColor: 'rgba(75, 181, 67, 0.5)', borderColor: 'rgba(75, 181, 67, 1)', borderWidth: 1 }] }, // Changed color
                options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { stepSize: 1, precision: 0 } } }, plugins: { legend: { display: false } } }
            });
            hideChartLoadingPlaceholder(ageGroupChartCanvas);
        } else if (ageGroupChartCanvas) {
            showChartMessage(ageGroupChartCanvas, 'No age group data available.');
        }

    } catch (error) {
        console.error('Error processing or rendering dashboard data:', error); // Changed log message for clarity
        if (totalEmployeesStatEl) totalEmployeesStatEl.textContent = 'Error';
        if (totalDepartmentsStatEl) totalDepartmentsStatEl.textContent = 'Error';
        if (averageAgeStatEl) averageAgeStatEl.textContent = 'Error';
        // Display general error message on dashboard
        const dashboardContainer = document.querySelector('.dashboard-container');
        if (dashboardContainer) {
            let errorDiv = dashboardContainer.querySelector('.dashboard-error-message'); // Try to find existing error div
            if (!errorDiv) {
                errorDiv = document.createElement('div');
                errorDiv.className = 'error-message dashboard-error-message'; // Add specific class
                errorDiv.style.textAlign = 'center';
                errorDiv.style.marginTop = '1rem';
                const header = dashboardContainer.querySelector('.dashboard-header');
                if (header) header.parentNode.insertBefore(errorDiv, header.nextSibling);
                else dashboardContainer.prepend(errorDiv);
            }
            errorDiv.textContent = `Failed to load dashboard data: ${error.message}. Please try again later.`;
        }
    }
}

function hideChartLoadingPlaceholder(canvasContextOrElement, selector = 'p') {
    let canvasElement = canvasContextOrElement.canvas ? canvasContextOrElement.canvas : canvasContextOrElement;
    if (!canvasElement) return;
    const placeholder = canvasElement.closest('.chart-placeholder');
    if (placeholder) {
        const pElement = placeholder.querySelector(selector);
        if (pElement) pElement.style.display = 'none';
    }
}

function showChartMessage(canvasContextOrElement, message, selector = 'p') {
    let canvasElement = canvasContextOrElement.canvas ? canvasContextOrElement.canvas : canvasContextOrElement;
    if (!canvasElement) return;
    const placeholder = canvasElement.closest('.chart-placeholder');
    if (placeholder) {
        const pElement = placeholder.querySelector(selector);
        if (pElement) {
            pElement.textContent = message;
            pElement.style.display = 'block';
        }
    }
}