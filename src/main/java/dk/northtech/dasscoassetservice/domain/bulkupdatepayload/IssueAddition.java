package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;


public record IssueAddition(
        String category,
        String name,
        String description,
        String status,
        Boolean solved,
        String notes
) {}