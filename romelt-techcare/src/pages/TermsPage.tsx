/**
 * ================================================================
 * ROMELT TECHCARE — TERMS OF SERVICE PAGE
 * ================================================================
 *
 * Purpose:
 * Defines preliminary terms governing website use and technology
 * service requests submitted to Romelt TechCare.
 *
 * Responsibilities:
 * - Describes customer and business responsibilities.
 * - Explains estimates, appointments, cancellations, and payments.
 * - Clarifies risks related to technical service and customer data.
 * - Establishes basic website-use expectations.
 *
 * Real-data integration:
 * These terms must be reviewed before production launch and updated
 * to match final contracts, warranties, payment policies, service
 * agreements, insurance requirements, and Iowa legal obligations.
 * ================================================================
 */

import { AlertTriangle, FileCheck2 } from "lucide-react";

import { Container } from "@/components/common/Container";
import { businessConfig } from "@/config/business.config";

const lastUpdated = "July 18, 2026";

export function TermsPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <Container>
          <div className="max-w-4xl py-16 sm:py-20">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
              <FileCheck2 className="h-7 w-7" aria-hidden="true" />
            </div>

            <p className="mt-6 text-sm font-extrabold uppercase tracking-[0.18em] text-brand-700">
              Terms of Service
            </p>

            <h1 className="mt-4 font-display text-4xl font-black tracking-tight text-navy-950 sm:text-5xl">
              Terms for website use and technology services
            </h1>

            <p className="mt-6 max-w-3xl text-lg leading-8 text-slate-600">
              These preliminary terms explain the conditions that apply when
              using the {businessConfig.name} website or requesting technology
              services.
            </p>

            <p className="mt-4 text-sm font-semibold text-slate-500">
              Last updated: {lastUpdated}
            </p>
          </div>
        </Container>
      </section>

      <section className="bg-slate-50 py-16">
        <Container>
          <div className="mx-auto max-w-4xl space-y-8">
            <TermsSection title="1. Acceptance of terms">
              <p>
                By using this website, submitting a contact form, requesting an
                appointment, or authorizing technology service, you agree to
                these terms and any written estimate or service agreement
                provided for the requested work.
              </p>
            </TermsSection>

            <TermsSection title="2. Service requests are not automatically accepted">
              <p>
                Submitting a booking or contact form does not guarantee an
                appointment, service availability, price, or acceptance of the
                requested work.
              </p>

              <p>
                A service request becomes confirmed only after
                {businessConfig.name} communicates acceptance and confirms the
                appointment or work arrangement.
              </p>
            </TermsSection>

            <TermsSection title="3. Estimates and pricing">
              <p>
                Website prices are starting estimates unless clearly stated
                otherwise. Final cost may depend on diagnosis, labor, travel,
                equipment, replacement parts, software, licensing, complexity,
                urgency, and the condition of the device or system.
              </p>

              <p>
                Additional work outside the agreed scope should be communicated
                before it is performed whenever reasonably possible.
              </p>
            </TermsSection>

            <TermsSection title="4. Appointments and access">
              <p>
                Customers are responsible for providing accurate contact
                details, safe access to the service location, and reasonable
                access to the equipment, network, accounts, or systems required
                to complete authorized work.
              </p>

              <p>
                A responsible adult should be available for residential or
                on-site service appointments unless another arrangement has been
                approved.
              </p>
            </TermsSection>

            <TermsSection title="5. Cancellations and rescheduling">
              <p>
                Customers should provide reasonable notice when cancelling or
                rescheduling an appointment.
              </p>

              <p>
                A cancellation, missed-appointment, or travel fee may be added
                later as the business establishes a formal appointment policy.
                Any such fee must be disclosed before it is enforced.
              </p>
            </TermsSection>

            <TermsSection title="6. Customer data and backups">
              <div className="flex items-start gap-4 rounded-2xl border border-amber-200 bg-amber-50 p-5">
                <AlertTriangle
                  className="mt-0.5 h-6 w-6 shrink-0 text-amber-700"
                  aria-hidden="true"
                />

                <p className="m-0 text-amber-950">
                  Customers should create a current backup of important files
                  before repair, troubleshooting, software installation,
                  operating-system work, storage changes, or device replacement.
                </p>
              </div>

              <p>
                Technology work can involve risks including data loss, hardware
                failure, software incompatibility, interrupted service, account
                lockout, or the discovery of pre-existing damage.
              </p>

              <p>
                Unless a separate written agreement states otherwise,
                {businessConfig.name} cannot guarantee recovery or preservation
                of data.
              </p>
            </TermsSection>

            <TermsSection title="7. Passwords and account access">
              <p>
                Customers may be asked to enter passwords themselves or provide
                temporary authorized access when required to perform a service.
              </p>

              <p>
                Customers should change temporary or shared passwords after the
                service is completed. Highly sensitive credentials should not be
                submitted through public website forms.
              </p>
            </TermsSection>

            <TermsSection title="8. Third-party products and services">
              <p>
                Some services may involve hardware manufacturers, internet
                providers, software vendors, cloud platforms, payment
                processors, or other third parties.
              </p>

              <p>
                Their products, warranties, subscriptions, availability,
                policies, and performance remain subject to their own terms and
                are not controlled by {businessConfig.name}.
              </p>
            </TermsSection>

            <TermsSection title="9. Payment">
              <p>
                Payment terms, accepted payment methods, deposits, taxes, parts
                charges, and invoice due dates will be communicated before or
                upon completion of the applicable service.
              </p>

              <p>
                Customers are responsible for approved charges and any
                authorized equipment, software, licensing, or third-party
                expenses.
              </p>
            </TermsSection>

            <TermsSection title="10. Warranty and follow-up support">
              <p>
                Any service warranty or follow-up period will be stated in the
                applicable estimate, invoice, or service agreement.
              </p>

              <p>
                New problems, unrelated failures, customer changes, malware,
                third-party updates, equipment defects, or internet-provider
                issues may require a separate service request.
              </p>
            </TermsSection>

            <TermsSection title="11. Acceptable website use">
              <p>You may not use this website to:</p>

              <ul>
                <li>Submit false, misleading, or fraudulent requests</li>
                <li>Attempt unauthorized access to website systems</li>
                <li>Upload malware or harmful content</li>
                <li>Interfere with website operation or security</li>
                <li>Request unlawful or unauthorized technical activity</li>
              </ul>
            </TermsSection>

            <TermsSection title="12. Limitation of liability">
              <p>
                To the extent permitted by applicable law,
                {businessConfig.name} will not be responsible for indirect,
                incidental, special, or consequential losses arising from
                website use, delayed service, equipment failure, data loss,
                third-party services, or events outside reasonable control.
              </p>
            </TermsSection>

            <TermsSection title="13. Changes to these terms">
              <p>
                These terms may be updated as pricing, scheduling, payments,
                warranties, customer portals, and service operations are
                finalized. The updated date on this page will reflect material
                changes.
              </p>
            </TermsSection>

            <TermsSection title="14. Contact">
              <p>
                Questions about these terms may be submitted through the website
                contact page.
              </p>
            </TermsSection>
          </div>
        </Container>
      </section>
    </>
  );
}

interface TermsSectionProps {
  title: string;
  children: React.ReactNode;
}

function TermsSection({ title, children }: TermsSectionProps) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <h2 className="font-display text-2xl font-extrabold text-navy-950">
        {title}
      </h2>

      <div className="mt-5 space-y-4 leading-8 text-slate-600 [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-6">
        {children}
      </div>
    </article>
  );
}
