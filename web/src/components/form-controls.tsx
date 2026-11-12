"use client";

import { useId, useState } from "react";
import {
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { parseDate } from "@internationalized/date";
import {
  Button,
  Calendar,
  CalendarCell,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHeader,
  CalendarHeaderCell,
  DateInput,
  DatePicker,
  DateSegment,
  Dialog,
  Group,
  Heading,
  I18nProvider,
  Label,
  ListBox,
  ListBoxItem,
  Popover,
  Select,
  SelectValue,
} from "react-aria-components";

interface FieldProps {
  name: string;
  label: string;
  defaultValue?: string;
  error?: string[];
}

export function SelectField({
  name,
  label,
  options,
  defaultValue = "",
  value,
  onChange,
  error,
  placeholder = "Selecione uma opção",
}: FieldProps & {
  options: { value: string; label: string }[];
  value?: string;
  onChange?(value: string): void;
  placeholder?: string;
}) {
  const [localValue, setLocalValue] = useState(defaultValue);
  const current = value ?? localValue;
  const errorId = useId();
  return (
    <Select
      className="field select-field"
      selectedKey={current || "__empty__"}
      onSelectionChange={(key) => {
        const next = key === "__empty__" || key === null ? "" : String(key);
        setLocalValue(next);
        onChange?.(next);
      }}
      isInvalid={Boolean(error)}
      aria-describedby={error ? errorId : undefined}
    >
      <Label>{label}</Label>
      <input type="hidden" name={name} value={current} />
      <Button className="select-trigger" data-field-name={name}>
        <SelectValue>
          {({ selectedText }) => selectedText || placeholder}
        </SelectValue>
        <ChevronDown size={17} aria-hidden="true" />
      </Button>
      <Popover
        className="control-popover select-popover"
        placement="bottom start"
        offset={6}
      >
        <ListBox className="select-options">
          {!options.some((option) => !option.value) && (
            <ListBoxItem
              id="__empty__"
              textValue={placeholder}
              className="select-option"
            >
              {placeholder}
              <Check size={16} aria-hidden="true" />
            </ListBoxItem>
          )}
          {options.map((option) => (
            <ListBoxItem
              id={option.value || "__empty__"}
              key={option.value}
              textValue={option.label}
              className="select-option"
            >
              {option.label}
              <Check size={16} aria-hidden="true" />
            </ListBoxItem>
          ))}
        </ListBox>
      </Popover>
      {error && (
        <p id={errorId} className="field-error">
          {error.join(" ")}
        </p>
      )}
    </Select>
  );
}

export function DateField({
  name,
  label,
  defaultValue = "",
  error,
  optional = false,
}: FieldProps & { optional?: boolean }) {
  const [value, setValue] = useState(() =>
    defaultValue ? parseDate(defaultValue) : null,
  );
  const [open, setOpen] = useState(false);
  const errorId = useId();
  return (
    <I18nProvider locale="pt-BR">
      <DatePicker
        className="field date-field"
        name={name}
        value={value}
        onChange={setValue}
        isOpen={open}
        onOpenChange={setOpen}
        isInvalid={Boolean(error)}
        validationBehavior="aria"
        aria-describedby={error ? errorId : undefined}
      >
        <Label>{label}</Label>
        <Group className="date-control">
          <DateInput className="date-input">
            {(segment) => (
              <DateSegment segment={segment} className="date-segment" />
            )}
          </DateInput>
          <Button
            className="calendar-trigger"
            aria-label={`Escolher ${label.toLowerCase()}`}
            data-field-name={name}
          >
            <CalendarDays size={19} aria-hidden="true" />
          </Button>
        </Group>
        <Popover
          className="control-popover calendar-popover"
          placement="bottom start"
          offset={6}
        >
          <Dialog className="calendar-dialog">
            <Calendar className="calendar" firstDayOfWeek="sun">
              <header>
                <Button
                  slot="previous"
                  className="calendar-nav"
                  aria-label="Mês anterior"
                >
                  <ChevronLeft size={18} />
                </Button>
                <Heading />
                <Button
                  slot="next"
                  className="calendar-nav"
                  aria-label="Próximo mês"
                >
                  <ChevronRight size={18} />
                </Button>
              </header>
              <CalendarGrid weekdayStyle="short">
                <CalendarGridHeader>
                  {(day) => <CalendarHeaderCell>{day}</CalendarHeaderCell>}
                </CalendarGridHeader>
                <CalendarGridBody>
                  {(date) => (
                    <CalendarCell date={date} className="calendar-day" />
                  )}
                </CalendarGridBody>
              </CalendarGrid>
            </Calendar>
            {optional && (
              <Button
                slot={null}
                className="calendar-clear"
                onPress={() => {
                  setValue(null);
                  setOpen(false);
                }}
              >
                Deixar sem data final
              </Button>
            )}
          </Dialog>
        </Popover>
        {error && (
          <p id={errorId} className="field-error">
            {error.join(" ")}
          </p>
        )}
      </DatePicker>
    </I18nProvider>
  );
}
