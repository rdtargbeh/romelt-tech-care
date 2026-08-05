/**
 * ================================================================
 * ROMELT TECHCARE — PRIVACY POLICY PAGE
 * ================================================================
 *
 * Purpose:
 * Explains how Romelt TechCare may collect, use, protect, and retain
 * information submitted through the public website.
 *
 * Responsibilities:
 * - Describes the types of information customers may provide.
 * - Explains how submitted information may be used.
 * - Describes basic privacy and security practices.
 * - Provides contact information for privacy questions.
 *
 * Real-data integration:
 * This policy must be reviewed and updated before production launch
 * to match the final booking system, analytics tools, payment
 * processor, customer portal, email provider, and backend retention
 * practices.
 * ================================================================
 */

import { LockKeyhole, Mail, ShieldCheck } from "lucide-react";

import { Container } from "@/components/common/Container";
import { businessConfig, businessEmailHref } from "@/config/business.config";

const lastUpdated = "July 18, 2026";

export function PrivacyPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <Container>
          <div className="max-w-4xl py-16 sm:py-20">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
              <LockKeyhole className="h-7 w-7" aria-hidden="true" />
            </div>

            <p className="mt-6 text-sm font-extrabold uppercase tracking-[0.18em] text-brand-700">
              Privacy Policy
            </p>

            <h1 className="mt-4 font-display text-4xl font-black tracking-tight text-navy-950 sm:text-5xl">
              How we handle your information
            </h1>

            <p className="mt-6 max-w-3xl text-lg leading-8 text-slate-600">
              This Privacy Policy explains how {businessConfig.name} may
              collect, use, protect, and retain information submitted through
              this website or during a service request.
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
            <PolicySection title="1. Information we may collect">
              <p>
                We may collect information that you voluntarily provide when
                contacting us, requesting service, booking an appointment, or
                communicating about a technology problem.
              </p>

              <p>This information may include:</p>

              <ul>
                <li>Your name</li>
                <li>Email address</li>
                <li>Telephone number</li>
                <li>Service address or general location</li>
                <li>Preferred appointment date or service method</li>
                <li>Information about a device or technology problem</li>
                <li>Messages, questions, and service-request details</li>
              </ul>
            </PolicySection>

            <PolicySection title="2. How we may use information">
              <p>
                Information submitted to {businessConfig.name} may be used to:
              </p>

              <ul>
                <li>Respond to questions and service inquiries</li>
                <li>Review and schedule technology service requests</li>
                <li>Communicate about appointments or requested work</li>
                <li>Provide estimates, invoices, and service documentation</li>
                <li>Improve customer service and website functionality</li>
                <li>Protect the website and prevent misuse or fraud</li>
                <li>Comply with applicable legal obligations</li>
              </ul>
            </PolicySection>

            <PolicySection title="3. Device access and technical information">
              <p>
                Some technology services may require temporary access to a
                computer, device, account, network, or system. Any such access
                should occur only with the customer&apos;s knowledge and
                permission.
              </p>

              <p>
                Customers should avoid sending passwords, financial account
                details, Social Security numbers, medical information, or other
                highly sensitive data through the public contact or booking
                forms.
              </p>
            </PolicySection>

            <PolicySection title="4. Cookies and website analytics">
              <p>
                The current website may use essential browser technologies
                required for navigation and normal operation.
              </p>

              <p>
                Analytics, advertising cookies, chat tools, or other tracking
                technologies may be added later. This policy will be updated
                before such tools are used in production.
              </p>
            </PolicySection>

            <PolicySection title="5. Sharing of information">
              <p>
                {businessConfig.name} does not intend to sell customer personal
                information.
              </p>

              <p>
                Information may be shared only when reasonably necessary with
                service providers that support business operations, such as
                hosting, email, payment, scheduling, accounting, or secure data
                storage providers.
              </p>

              <p>
                Information may also be disclosed when required by law or when
                necessary to protect customers, the business, or others from
                fraud, abuse, or security threats.
              </p>
            </PolicySection>

            <PolicySection title="6. Data security">
              <div className="flex items-start gap-4 rounded-2xl border border-brand-100 bg-brand-50 p-5">
                <ShieldCheck
                  className="mt-0.5 h-6 w-6 shrink-0 text-brand-700"
                  aria-hidden="true"
                />

                <p className="m-0">
                  Reasonable administrative and technical safeguards will be
                  used to protect customer information. No website, storage
                  system, or internet transmission can be guaranteed to be
                  completely secure.
                </p>
              </div>
            </PolicySection>

            <PolicySection title="7. Data retention">
              <p>
                Information may be retained for as long as reasonably necessary
                to provide services, maintain business records, resolve
                disputes, support warranties, prevent fraud, and meet legal or
                accounting obligations.
              </p>
            </PolicySection>

            <PolicySection title="8. Your choices">
              <p>
                You may contact {businessConfig.name} to request correction or
                deletion of personal information, subject to legal, accounting,
                security, and legitimate business-record requirements.
              </p>
            </PolicySection>

            <PolicySection title="9. Children’s privacy">
              <p>
                This website is intended for adults seeking technology services.
                It is not designed to knowingly collect personal information
                directly from children without the involvement of a parent or
                legal guardian.
              </p>
            </PolicySection>

            <PolicySection title="10. Changes to this policy">
              <p>
                This Privacy Policy may be revised as the website, booking
                system, payment services, customer portal, and business
                operations develop. The updated date shown on this page will be
                changed when material revisions are made.
              </p>
            </PolicySection>

            <PolicySection title="11. Privacy questions">
              <p>
                Questions or requests concerning this policy may be submitted
                through the website contact page or by email.
              </p>

              <div className="mt-5 flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4">
                <Mail
                  className="mt-0.5 h-5 w-5 shrink-0 text-brand-700"
                  aria-hidden="true"
                />

                {businessEmailHref ? (
                  <a
                    href={businessEmailHref}
                    className="break-all font-bold text-brand-700 hover:text-brand-900"
                  >
                    {businessConfig.email}
                  </a>
                ) : (
                  <span>Contact information will be added before launch.</span>
                )}
              </div>
            </PolicySection>
          </div>
        </Container>
      </section>
    </>
  );
}

interface PolicySectionProps {
  title: string;
  children: React.ReactNode;
}

function PolicySection({ title, children }: PolicySectionProps) {
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
