package com.defence.logistics.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.defence.logistics.service.MicroserviceClient;
import com.defence.logistics.service.MicroserviceClient.ServiceName;
import com.defence.logistics.auth.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final MicroserviceClient api;
    private final AccountService accounts;

    public AdminController(MicroserviceClient api, AccountService accounts) { this.api = api; this.accounts = accounts; }

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping({"/login", "/auth/login"})
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register() { return "auth/register"; }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String fullName, @RequestParam String username,
            @RequestParam String email, @RequestParam String password,
            @RequestParam String confirmPassword, RedirectAttributes flash) {
        if (!password.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }
        try {
            accounts.registerRequester(fullName, username, email, password);
            flash.addFlashAttribute("success", "Account created. Sign in with your new credentials.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, HttpSession session) {
        List<Map<String, Object>> users = api.getList(ServiceName.RESOURCE, "/api/users");
        List<Map<String, Object>> personnel = api.getList(ServiceName.RESOURCE, "/api/personnel");
        List<Map<String, Object>> vehicles = api.getList(ServiceName.RESOURCE, "/api/vehicles");
        List<Map<String, Object>> inventory = api.getList(ServiceName.INVENTORY, "/api/inventory");
        List<Map<String, Object>> equipment = api.getList(ServiceName.INVENTORY, "/api/equipment");
        List<Map<String, Object>> requests = api.getList(ServiceName.LOGISTICS, "/api/requests");
        List<Map<String, Object>> maintenance = api.getList(ServiceName.MAINTENANCE, "/api/maintenance");
        model.addAttribute("user", currentUser(session));
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("dashboardStats", Map.of(
                "totalUsers", users.size(), "totalPersonnel", personnel.size(), "totalVehicles", vehicles.size(),
                "totalEquipment", equipment.size(), "inventoryItems", inventory.size(),
                "activeRequests", countActiveRequests(requests), "underMaintenance", maintenance.size()));
        model.addAttribute("recentActivities", List.of());
        model.addAttribute("integrationNote", "Dashboard totals are live counts returned by the configured microservices.");
        return "admin/admin_dashboard";
    }

    @GetMapping("/logistics/dashboard")
    public String logisticsDashboard(Model model, HttpSession session) {
        List<Map<String, Object>> requests = api.getList(ServiceName.LOGISTICS, "/api/requests");
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("pendingRequests", requests.stream().filter(r -> "PENDING".equalsIgnoreCase(String.valueOf(r.get("status")))).toList());
        model.addAttribute("dashboardStats", Map.of(
                "pendingRequests", countStatus(requests, "PENDING"),
                "approvedToday", countStatus(requests, "APPROVED"),
                "inProgress", countStatus(requests, "IN_PROGRESS"),
                "completed", countStatus(requests, "COMPLETED")));
        model.addAttribute("integrationNote", "Request data is fetched from Logistics_Service.");
        return "logistics/dashboard";
    }

    @GetMapping("/requester/dashboard")
public String requesterDashboard(Model model, HttpSession session) {
    model.addAttribute("currentUser", currentUser(session));

    List<Map<String, Object>> requests =
            api.getList(ServiceName.LOGISTICS, "/api/requests/my");

    if (requests == null) {
        requests = List.of();
    }

    model.addAttribute("myRequests", requests);

    model.addAttribute("dashboardStats", Map.of(
            "totalRequests", requests.size(),
            "pending", countStatus(requests, "PENDING"),
            "approved", countStatus(requests, "APPROVED"),
            "completed", countStatus(requests, "COMPLETED")
    ));

    return "requester/dashboard";
}

    @GetMapping("/vehicle_mgmnt")
    public String vehicleManagement(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("vehicleList", api.getList(ServiceName.RESOURCE, "/api/vehicles"));
        return "admin/vehicle_management";
    }

    @GetMapping("/admin/users")
    public String users(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("userList", api.getList(ServiceName.RESOURCE, "/api/users"));
        return "admin/user_management";
    }

    @GetMapping("/admin/roles")
    public String roles(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("roles", List.of(
                role("GUEST", "Guest", "Unauthenticated visitor", "View public landing page and sign-in/register pages", "No domain data changes"),
                role("ADMIN", "Administrator", "System administrator", "Manage users, review service status and oversee platform", "Administrative operations; enforce through identity/security service"),
                role("LOGISTIC_OFFICER", "Logistics Officer", "Logistics operations staff", "Review requests, coordinate allocation and update fulfilment", "Logistics_Service APIs"),
                role("QUARTERMASTER", "Quartermaster / Unit Personnel", "Defence unit supply requester", "Submit resource requests and track unit requests", "Own-unit requests only; enforce ownership in backend")));
        return "admin/user_roles";
    }

    @GetMapping("/admin/inventory")
    public String inventory(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("inventoryList", api.getList(ServiceName.INVENTORY, "/api/inventory"));
        return "admin/inventory_management";
    }

    @GetMapping("/admin/maintenance")
    public String maintenance(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("maintenanceList", api.getList(ServiceName.MAINTENANCE, "/api/maintenance"));
        return "admin/maintenance_management";
    }

    @GetMapping("/admin/equipment")
    public String equipment(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("inventoryList", api.getList(ServiceName.INVENTORY, "/api/equipment"));
        return "admin/inventory_management";
    }

    @GetMapping({"/admin/requests", "/logistics/requests"})
    public String requests(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("pendingRequests", api.getList(ServiceName.LOGISTICS, "/api/requests"));
        model.addAttribute("myRequests", api.getList(ServiceName.LOGISTICS, "/api/requests"));
        model.addAttribute("requestList", api.getList(ServiceName.LOGISTICS, "/api/requests"));
        return "logistics/dashboard";
    }

    @GetMapping("/admin/reports")
    public String reports(@RequestParam(required = false) String type, @RequestParam(required = false) String status, Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("reportData", api.getList(ServiceName.LOGISTICS, "/api/reports"));
        model.addAttribute("type", type); model.addAttribute("status", status);
        return "admin/reports";
    }

    @GetMapping("/requester/requests/create")
    public String createRequest(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("newRequest", new LinkedHashMap<String, Object>());
        return "requester/create_request";
    }

    @PostMapping("/requester/requests/create")
    public String submitRequest(@RequestParam Map<String, String> form, RedirectAttributes flash) {
        boolean ok = api.post(ServiceName.LOGISTICS, "/api/requests", new LinkedHashMap<>(form));
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Request submitted to Logistics_Service." : "Could not submit request. Check Logistics_Service availability and API path configuration.");
        return "redirect:/requester/dashboard";
    }

    @GetMapping("/logistics/requests/{id}")
    public String requestDetails(@PathVariable String id, Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        Map<String, Object> request = api.getOne(ServiceName.LOGISTICS, "/api/requests/" + id);
        request.putIfAbsent("id", id); request.putIfAbsent("purpose", "Request details unavailable from service");
        request.putIfAbsent("requestDate", LocalDate.now().toString()); request.putIfAbsent("requestedBy", "Not returned by service");
        request.putIfAbsent("requiredFrom", "—"); request.putIfAbsent("requiredTo", "—"); request.putIfAbsent("type", "—");
        model.addAttribute("request", request);
        return "logistics/request_details";
    }

    @PostMapping("/logistics/requests/{id}/process")
    public String processRequest(@PathVariable String id, @RequestParam String action, RedirectAttributes flash) {
        boolean ok = api.post(ServiceName.LOGISTICS, "/api/requests/" + id + "/" + action, Map.of("action", action));
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Request action sent to Logistics_Service." : "Request action failed; verify the configured API endpoint.");
        return "redirect:/logistics/requests/" + id;
    }

    @GetMapping("/logistics/requests/{id}/allocate")
    public String allocate(@PathVariable String id, Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session)); model.addAttribute("requestId", id);
        model.addAttribute("availableVehicles", api.getList(ServiceName.RESOURCE, "/api/vehicles/available"));
        return "logistics/resource_allocation";
    }

    @PostMapping("/logistics/requests/{id}/allocate/confirm")
    public String confirmAllocation(@PathVariable String id, @RequestParam Map<String, String> form, RedirectAttributes flash) {
        boolean ok = api.post(ServiceName.LOGISTICS, "/api/requests/" + id + "/allocate", new LinkedHashMap<>(form));
        flash.addFlashAttribute(ok ? "success" : "error", ok ? "Allocation sent to Logistics_Service." : "Allocation failed; verify the service API endpoint.");
        return "redirect:/logistics/requests/" + id;
    }

    @GetMapping("/requester/requests/{id}/track")
    public String track(@PathVariable String id, Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUser(session));
        Map<String, Object> request = api.getOne(ServiceName.LOGISTICS, "/api/requests/" + id);
        request.putIfAbsent("id", id); model.addAttribute("request", request);
        return "requester/track_status";
    }

    @GetMapping("/admin/vehicles/add")
    public String addVehicle(Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); model.addAttribute("formTitle", "Add Vehicle"); return "admin/vehicle_form"; }
    @GetMapping("/admin/vehicles/edit/{id}")
    public String editVehicle(@PathVariable String id, Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); model.addAttribute("formTitle", "Edit Vehicle"); model.addAttribute("record", api.getOne(ServiceName.RESOURCE, "/api/vehicles/" + id)); model.addAttribute("recordId", id); return "admin/vehicle_form"; }
    @PostMapping("/admin/vehicles/save")
    public String saveVehicle(@RequestParam Map<String, String> form, @RequestParam(required = false) String id, RedirectAttributes flash) { boolean ok = id == null || id.isBlank() ? api.post(ServiceName.RESOURCE, "/api/vehicles", new LinkedHashMap<>(form)) : api.put(ServiceName.RESOURCE, "/api/vehicles/" + id, new LinkedHashMap<>(form)); flash.addFlashAttribute(ok ? "success" : "error", ok ? "Vehicle saved." : "Vehicle service rejected the request or is unavailable."); return "redirect:/vehicle_mgmnt"; }
    @GetMapping("/admin/users/add")
    public String addUser(Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); model.addAttribute("formTitle", "Add User"); return "admin/user_form"; }
    @GetMapping("/admin/users/edit/{id}")
    public String editUser(@PathVariable String id, Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); model.addAttribute("formTitle", "Edit User"); model.addAttribute("recordId", id); model.addAttribute("record", api.getOne(ServiceName.RESOURCE, "/api/users/" + id)); return "admin/user_form"; }
    @PostMapping("/admin/users/save")
    public String saveUser(@RequestParam Map<String, String> form, @RequestParam(required = false) String id, RedirectAttributes flash) { boolean ok = id == null || id.isBlank() ? api.post(ServiceName.RESOURCE, "/api/users", new LinkedHashMap<>(form)) : api.put(ServiceName.RESOURCE, "/api/users/" + id, new LinkedHashMap<>(form)); flash.addFlashAttribute(ok ? "success" : "error", ok ? "User saved." : "User service rejected the request or is unavailable."); return "redirect:/admin/users"; }

    @GetMapping("/admin/inventory/edit/{id}")
    public String editInventory(@PathVariable String id, Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); model.addAttribute("recordId", id); model.addAttribute("record", api.getOne(ServiceName.INVENTORY, "/api/inventory/" + id)); return "admin/inventory_form"; }
    @PostMapping("/admin/inventory/save")
    public String saveInventory(@RequestParam Map<String, String> form, @RequestParam(required = false) String id, RedirectAttributes flash) { boolean ok = id == null || id.isBlank() ? api.post(ServiceName.INVENTORY, "/api/inventory", new LinkedHashMap<>(form)) : api.put(ServiceName.INVENTORY, "/api/inventory/" + id, new LinkedHashMap<>(form)); flash.addFlashAttribute(ok ? "success" : "error", ok ? "Inventory item saved." : "Inventory service rejected the request or is unavailable."); return "redirect:/admin/inventory"; }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) { model.addAttribute("currentUser", currentUser(session)); return "shared/profile"; }

    private Map<String, Object> currentUser(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
            return Map.of("username", "Guest", "role", "GUEST");
        String role = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("QUARTERMASTER");
        return Map.of("username", authentication.getName(), "role", role);
    }
    private long countStatus(List<Map<String, Object>> rows, String expected) {
        return rows.stream().filter(row -> expected.equalsIgnoreCase(String.valueOf(row.get("status")).toUpperCase().replace(' ', '_'))).count();
    }
    private long countActiveRequests(List<Map<String, Object>> rows) {
        return rows.stream().filter(row -> {
            String status = String.valueOf(row.get("status")).toUpperCase().replace(' ', '_');
            return status.equals("PENDING") || status.equals("APPROVED") || status.equals("IN_PROGRESS");
        }).count();
    }
    private Map<String, Object> role(String code, String name, String audience, String permissions, String boundary) {
        return Map.of("code", code, "name", name, "audience", audience, "permissions", permissions, "boundary", boundary);
    }
}
